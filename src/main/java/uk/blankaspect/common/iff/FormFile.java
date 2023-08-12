/*====================================================================*\

FormFile.java

Abstract IFF form file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.OverlappingFileLockException;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TempFileException;

import uk.blankaspect.common.filesystem.FilenameUtils;

//----------------------------------------------------------------------


// ABSTRACT IFF FORM FILE CLASS


public abstract class FormFile
{

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ERROR IDENTIFIERS


	protected enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FILE_DOES_NOT_EXIST
		("The file does not exist."),

		FAILED_TO_OPEN_FILE
		("Failed to open the file."),

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		FAILED_TO_LOCK_FILE
		("Failed to lock the file."),

		ERROR_READING_FILE
		("An error occurred when reading the file."),

		ERROR_WRITING_FILE
		("An error occurred when writing the file."),

		FILE_ACCESS_NOT_PERMITTED
		("Access to the file was not permitted."),

		FAILED_TO_CREATE_DIRECTORY
		("Failed to create the directory."),

		FAILED_TO_CREATE_TEMPORARY_FILE
		("Failed to create a temporary file."),

		FAILED_TO_DELETE_FILE
		("Failed to delete the existing file."),

		FAILED_TO_RENAME_FILE
		("Failed to rename the temporary file to the specified filename."),

		INCORRECT_FORMAT
		("The file is not a valid '%1' file."),

		MALFORMED_FILE
		("The file is malformed."),

		ILLEGAL_CHUNK_ID
		("The file contains an illegal chunk identifier.");

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ErrorId(String message)
		{
			this.message = message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : AppException.IId interface
	////////////////////////////////////////////////////////////////////

		public String getMessage()
		{
			return message;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// CHUNK READER INTERFACE


	public interface IChunkReader
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		void beginReading(RandomAccessFile raFile,
						  IffId            typeId,
						  int              size)
			throws AppException;

		//--------------------------------------------------------------

		void read(RandomAccessFile raFile,
				  IffId            id,
				  int              size)
			throws AppException, IOException;

		//--------------------------------------------------------------

		void endReading(RandomAccessFile raFile)
			throws AppException, IOException;

		//--------------------------------------------------------------

	}

	//==================================================================


	// CHUNK WRITER INTERFACE


	public interface IChunkWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		void beginWriting(RandomAccessFile raFile)
			throws AppException, IOException;

		//--------------------------------------------------------------

		IffId getNextId();

		//--------------------------------------------------------------

		void write(RandomAccessFile raFile,
				   IffId            id)
			throws AppException, IOException;

		//--------------------------------------------------------------

		void endWriting(RandomAccessFile raFile)
			throws AppException, IOException;

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// ID LIST CLASS


	public static class IdList
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public IdList()
		{
			chunkIds = new ArrayList<>();
		}

		//--------------------------------------------------------------

		public IdList(IffId       groupTypeId,
					  List<IffId> chunkIds)
		{
			this.groupTypeId = groupTypeId;
			this.chunkIds = chunkIds;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	IffId		groupTypeId;
		public	List<IffId>	chunkIds;

	}

	//==================================================================


	// CHUNK LISTER CLASS


	private static class ChunkLister
		implements IChunkReader
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ChunkLister()
		{
			idList = new IdList();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ChunkReader interface
	////////////////////////////////////////////////////////////////////

		public void beginReading(RandomAccessFile raFile,
								 IffId            typeId,
								 int              size)
		{
			idList.groupTypeId = typeId;
		}

		//--------------------------------------------------------------

		public void read(RandomAccessFile raFile,
						 IffId            id,
						 int              size)
		{
			idList.chunkIds.add(id);
		}

		//--------------------------------------------------------------

		public void endReading(RandomAccessFile raFile)
		{
			// do nothing
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	IdList	idList;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	IffId	groupId;
	private	File	file;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FormFile(IffId groupId,
					File  file)
	{
		this.groupId = groupId;
		this.file = file;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	protected abstract int getChunkSize(byte[] sizeBytes,
										int    offset);

	//------------------------------------------------------------------

	protected abstract void writeGroupHeader(DataOutput dataOutput,
											 IffId      groupId,
											 IffId      typeId,
											 int        size)
		throws IOException;

	//------------------------------------------------------------------

	protected abstract void writeChunkHeader(DataOutput dataOutput,
											 IffId      id,
											 int        size)
		throws IOException;

	//------------------------------------------------------------------

	protected abstract void putSize(int    size,
									byte[] buffer);

	//------------------------------------------------------------------

	protected abstract Group createGroup(byte[] header);

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public IffId getGroupId()
	{
		return groupId;
	}

	//------------------------------------------------------------------

	public File getFile()
	{
		return file;
	}

	//------------------------------------------------------------------

	public IdList getIds()
		throws AppException
	{
		ChunkLister lister = new ChunkLister();
		read(lister);
		return lister.idList;
	}

	//------------------------------------------------------------------

	public void read(IChunkReader chunkReader)
		throws AppException
	{
		// Test whether file exists
		if (!file.isFile())
			throw new FileException(ErrorId.FILE_DOES_NOT_EXIST, file);

		// Read file
		RandomAccessFile raFile = null;
		try
		{
			// Open file
			try
			{
				raFile = new RandomAccessFile(file, "r");
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, file, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, file, e);
			}

			// Lock file
			try
			{
				if (raFile.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, file, e);
			}

			// Process file
			try
			{
				// Read group header
				byte[] buffer = new byte[Group.HEADER_SIZE];
				raFile.readFully(buffer);

				// Test group
				Group group = null;
				try
				{
					group = createGroup(buffer);
					if (!group.getGroupId().equals(groupId))
						throw new IllegalArgumentException();
				}
				catch (IllegalArgumentException e)
				{
					throw new FileException(ErrorId.INCORRECT_FORMAT, file, groupId.toString());
				}

				// Test group size
				int groupSize = getChunkSize(buffer, IffId.SIZE);
				if (groupSize > raFile.length() - Chunk.HEADER_SIZE)
					throw new FileException(ErrorId.MALFORMED_FILE, file);

				// Begin reading chunks
				chunkReader.beginReading(raFile, group.getTypeId(), groupSize);

				// Initialise variables
				long groupOffset = IffId.SIZE;
				buffer = new byte[Chunk.HEADER_SIZE];

				// Call reader on each chunk in group
				while (groupOffset < groupSize)
				{
					// Seek next chunk
					raFile.seek(Chunk.HEADER_SIZE + groupOffset);

					// Test whether chunk header extends beyond end of group
					if (groupOffset + Chunk.HEADER_SIZE > groupSize)
						throw new FileException(ErrorId.MALFORMED_FILE, file);

					// Read chunk header
					raFile.readFully(buffer);

					// Get chunk ID and size
					IffId chunkId = null;
					try
					{
						chunkId = new IffId(buffer);
					}
					catch (IllegalArgumentException e)
					{
						throw new FileException(ErrorId.ILLEGAL_CHUNK_ID, file);
					}
					int chunkSize = getChunkSize(buffer, IffId.SIZE);

					// Test whether chunk extends beyond end of group
					groupOffset += Chunk.HEADER_SIZE;
					if (chunkSize > groupSize - groupOffset)
						throw new IffException(ErrorId.MALFORMED_FILE, file, chunkId);

					// Read chunk
					chunkReader.read(raFile, chunkId, chunkSize);

					// Increment group offset
					groupOffset += chunkSize;
					if ((chunkSize & 1) != 0)
						++groupOffset;
				}

				// End reading chunks
				chunkReader.endReading(raFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_READING_FILE, file, e);
			}

			// Close file
			try
			{
				raFile.close();
				raFile = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, file, e);
			}
		}
		catch (AppException e)
		{
			// Close file
			try
			{
				if (raFile != null)
					raFile.close();
			}
			catch (IOException e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	public void write(IffId        typeId,
					  IChunkWriter chunkWriter)
		throws AppException
	{
		RandomAccessFile raFile = null;
		File tempFile = null;
		boolean oldFileDeleted = false;

		try
		{
			// Create parent directory of output file
			File directory = file.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new FileException(ErrorId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(file);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new AppException(ErrorId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open temporary file for reading and writing
			try
			{
				raFile = new RandomAccessFile(tempFile, "rw");
			}
			catch (FileNotFoundException e)
			{
				throw new FileException(ErrorId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock file
			try
			{
				if (raFile.getChannel().tryLock() == null)
					throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Write file
			try
			{
				// Write group header
				writeGroupHeader(raFile, groupId, typeId, 0);

				// Begin writing chunks
				chunkWriter.beginWriting(raFile);

				// Write chunks
				while (true)
				{
					// Get ID of next chunk
					IffId id = chunkWriter.getNextId();
					if (id == null)
						break;

					// Write chunk
					if (!id.equals(new IffId()))
					{
						// Write chunk header
						long chunkOffset = raFile.getFilePointer();
						writeChunkHeader(raFile, id, 0);

						// Write chunk data
						chunkWriter.write(raFile, id);

						// Pad chunk to even length
						long dataSize = raFile.length() - chunkOffset - Chunk.HEADER_SIZE;
						if ((dataSize & 1) != 0)
							raFile.write(0);

						// Write size of data into chunk header
						writeSize(raFile, chunkOffset + IffId.SIZE, (int)dataSize);

						// Seek end of file
						raFile.seek(raFile.length());
					}
				}

				// End writing chunks
				chunkWriter.endWriting(raFile);

				// Write group size into FORM header
				writeSize(raFile, IffId.SIZE, (int)raFile.length() - Chunk.HEADER_SIZE);
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_WRITING_FILE, file, e);
			}

			// Close file
			try
			{
				raFile.close();
				raFile = null;
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, tempFile, e);
			}

			// Delete any existing file
			try
			{
				if (file.exists() && !file.delete())
					throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, file);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new FileException(ErrorId.FAILED_TO_DELETE_FILE, file, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(file))
					throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, file, tempFile);
			}
			catch (SecurityException e)
			{
				throw new TempFileException(ErrorId.FAILED_TO_RENAME_FILE, file, e, tempFile);
			}
		}
		catch (AppException e)
		{
			// Close file
			try
			{
				if (raFile != null)
					raFile.close();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	private void writeSize(RandomAccessFile raFile,
						   long             offset,
						   int              size)
		throws IOException
	{
		raFile.seek(offset);
		byte[] sizeBuffer = new byte[Chunk.SIZE_SIZE];
		putSize(size, sizeBuffer);
		raFile.write(sizeBuffer);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
