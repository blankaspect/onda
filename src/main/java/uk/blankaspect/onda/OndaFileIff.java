/*====================================================================*\

OndaFileIff.java

ONDA lossless audio compression IFF file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.OverlappingFileLockException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.blankaspect.common.bytedata.IByteDataOutputStream;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.iff.FormFile;
import uk.blankaspect.common.iff.IffFormFile;
import uk.blankaspect.common.iff.IffId;

import uk.blankaspect.common.nlf.Document;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// ONDA LOSSLESS AUDIO COMPRESSION IFF FILE CLASS


class OndaFileIff
	implements OndaFileReader
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	IffId	FORM_ID				= new IffId("FORM");
	@SuppressWarnings("unused")
	private static final    IffId   TYPE_ID             = new IffId("ONDA");
	private static final	IffId	ATTRIBUTES_ID		= new IffId("ATTR");
	private static final	IffId	PRIVATE_DATA_ID		= new IffId("PRVT");
	@SuppressWarnings("unused")
	private static final    IffId   DATA_BLOCK_SIZE_ID  = new IffId("DBSZ");
	private static final	IffId	DATA_ID				= new IffId("DATA");

	private static final	int	READ_ATTRIBUTES		= 1 << 0;
	private static final	int	READ_PRIVATE_DATA	= 1 << 1;
	private static final	int	READ_DATA			= 1 << 2;

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FAILED_TO_OPEN_FILE
		("Failed to open the file."),

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		FAILED_TO_LOCK_FILE
		("Failed to lock the file."),

		ERROR_READING_FILE
		("An error occurred when reading the file."),

		FILE_ACCESS_NOT_PERMITTED
		("Access to the file was not permitted."),

		MALFORMED_FILE
		("The file is malformed."),

		NO_ATTRIBUTES_CHUNK
		("The file does not have an attributes chunk."),

		NO_ATTRIBUTES_CHUNK_BEFORE_DATA_CHUNK
		("There is no attributes chunk before the data chunk."),

		MULTIPLE_ATTRIBUTES_CHUNKS
		("The file has more than one attributes chunk."),

		INVALID_ATTRIBUTES_CHUNK
		("The attributes chunk is not valid."),

		PRIVATE_CHUNK_AFTER_DATA_CHUNK
		("There is a private data chunk after the data chunk."),

		MULTIPLE_PRIVATE_CHUNKS
		("The file has more than one private data chunk."),

		NO_DATA_CHUNK
		("The file does not have a data chunk."),

		MULTIPLE_DATA_CHUNKS
		("The file has more than one data chunk."),

		UNSUPPORTED_VERSION
		("The version of the file (%1) is not supported by this program."),

		NOT_AN_ONDA_FILE
		("The file is not an Onda file."),

		NOT_ENOUGH_MEMORY
		("There was not enough memory to read the file.");

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
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CHUNK READER CLASS


	private class ChunkReader
		implements FormFile.IChunkReader
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ChunkReader(int                   readKind,
							IByteDataOutputStream outStream)
		{
			this.readKind = readKind;
			this.outStream = outStream;
			ids = new ArrayList<>();
			if ((readKind & READ_ATTRIBUTES) != 0)
				attributes = null;
			if ((readKind & READ_PRIVATE_DATA) != 0)
				privateData = null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FormFile.IChunkReader interface
	////////////////////////////////////////////////////////////////////

		public void beginReading(RandomAccessFile raFile,
								 IffId            typeId,
								 int              size)
		{
			ids.clear();
		}

		//--------------------------------------------------------------

		public void read(RandomAccessFile raFile,
						 IffId            id,
						 int              size)
			throws AppException, IOException
		{
			if (id.equals(ATTRIBUTES_ID))
			{
				if (ids.contains(ATTRIBUTES_ID))
					throw new FileException(ErrorId.MULTIPLE_ATTRIBUTES_CHUNKS, file);

				if ((readKind & READ_ATTRIBUTES) != 0)
					readAttributes(raFile, size);
				ids.add(id);
			}

			else if (id.equals(PRIVATE_DATA_ID))
			{
				if (ids.contains(DATA_ID))
					throw new FileException(ErrorId.PRIVATE_CHUNK_AFTER_DATA_CHUNK, file);

				if (ids.contains(PRIVATE_DATA_ID))
					throw new FileException(ErrorId.MULTIPLE_PRIVATE_CHUNKS, file);

				if ((readKind & READ_PRIVATE_DATA) != 0)
					readPrivateData(raFile, size);
				ids.add(id);
			}

			else if (id.equals(DATA_ID))
			{
				if (!ids.contains(ATTRIBUTES_ID))
					throw new FileException(ErrorId.NO_ATTRIBUTES_CHUNK_BEFORE_DATA_CHUNK, file);

				if (ids.contains(DATA_ID))
					throw new FileException(ErrorId.MULTIPLE_DATA_CHUNKS, file);

				if ((readKind & READ_DATA) != 0)
					readData(raFile, size);
				ids.add(id);
			}
		}

		//--------------------------------------------------------------

		public void endReading(RandomAccessFile raFile)
			throws AppException
		{
			if (!ids.contains(ATTRIBUTES_ID))
				throw new FileException(ErrorId.NO_ATTRIBUTES_CHUNK, file);
			if (!ids.contains(DATA_ID))
				throw new FileException(ErrorId.NO_DATA_CHUNK, file);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void readAttributes(RandomAccessFile raFile,
									int              size)
			throws AppException, IOException
		{
			// Test version
			if (size < OndaFile.Attributes.VERSION_SIZE)
				throw new FileException(ErrorId.INVALID_ATTRIBUTES_CHUNK, file);
			byte[] buffer = new byte[OndaFile.Attributes.SIZE];
			raFile.readFully(buffer, 0, OndaFile.Attributes.VERSION_SIZE);
			int version = NumberCodec.bytesToIntBE(buffer, 0, OndaFile.Attributes.VERSION_SIZE);
			if ((version < OndaFile.MIN_SUPPORTED_VERSION) || (version > OndaFile.MAX_SUPPORTED_VERSION))
				throw new FileException(ErrorId.UNSUPPORTED_VERSION, file, Integer.toString(version));

			// Read attributes
			if (size != OndaFile.Attributes.SIZE)
				throw new FileException(ErrorId.INVALID_ATTRIBUTES_CHUNK, file);
			raFile.readFully(buffer, OndaFile.Attributes.VERSION_SIZE,
							 OndaFile.Attributes.SIZE - OndaFile.Attributes.VERSION_SIZE);
			try
			{
				attributes = new OndaFile.Attributes(buffer, 0);
			}
			catch (AppException e)
			{
				throw new FileException(e, file);
			}
		}

		//--------------------------------------------------------------

		private void readPrivateData(RandomAccessFile raFile,
									 int              size)
			throws AppException, IOException
		{
			try
			{
				byte[] buffer = new byte[size];
				raFile.readFully(buffer);
				privateData = buffer;
			}
			catch (OutOfMemoryError e)
			{
				throw new FileException(ErrorId.NOT_ENOUGH_MEMORY, file);
			}
		}

		//--------------------------------------------------------------

		private void readData(RandomAccessFile raFile,
							  int              size)
			throws AppException, IOException
		{
			// Open compressed data input
			OndaDataInput compressedDataInput = new OndaDataInput(size, attributes.numChannels,
																  attributes.bitsPerSample,
																  attributes.keyLength, raFile);

			// Read compressed data and write them to output stream
			int bytesPerSample = attributes.getBytesPerSample();
			int numSampleFrames = (int)attributes.numSampleFrames;
			int[] inBuffer = new int[attributes.blockLength * attributes.numChannels];
			byte[] outBuffer = new byte[inBuffer.length * bytesPerSample];
			int sampleFrameIndex = 0;
			while (sampleFrameIndex < numSampleFrames)
			{
				try
				{
					// Read sample data from input
					int readNumSampleFrames = Math.min(numSampleFrames - sampleFrameIndex,
													   attributes.blockLength);
					int readLength = readNumSampleFrames * attributes.numChannels;
					compressedDataInput.readBlock(inBuffer, 0, readLength);
					sampleFrameIndex += readNumSampleFrames;

					// Write sample data to output stream
					int offset = 0;
					for (int i = 0; i < readLength; i++)
					{
						int sampleValue = inBuffer[i];
						for (int j = 0; j < bytesPerSample; j++)
						{
							outBuffer[offset++] = (byte)sampleValue;
							sampleValue >>= 8;
						}
					}
					outStream.write(outBuffer, 0, offset);
				}
				catch (IOException e)
				{
					throw new FileException(ErrorId.MALFORMED_FILE, file);
				}
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int						readKind;
		private	IByteDataOutputStream	outStream;
		private	List<IffId>				ids;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public OndaFileIff(File file)
	{
		this.file = file;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static OndaFileReader.Kind getFileKind(File file)
		throws AppException
	{
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

			// Read IFF form identifier
			OndaFileReader.Kind fileKind = null;
			try
			{
				if (raFile.length() >= IffId.SIZE)
				{
					byte[] buffer = new byte[IffId.SIZE];
					raFile.readFully(buffer);

					if (Arrays.equals(buffer, Document.FILE_ID))
						fileKind = OndaFileReader.Kind.NLF;
					else
					{
						try
						{
							if (new IffId(buffer).equals(FORM_ID))
								fileKind = OndaFileReader.Kind.IFF;
						}
						catch (IllegalArgumentException e)
						{
							// ignore
						}
					}
				}

				if (fileKind == null)
					throw new FileException(ErrorId.NOT_AN_ONDA_FILE, file);
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

			return fileKind;
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

////////////////////////////////////////////////////////////////////////
//  Instance methods : OndaFileReader interface
////////////////////////////////////////////////////////////////////////

	public byte[] getPrivateData()
	{
		return privateData;
	}

	//------------------------------------------------------------------

	public OndaFile.Attributes readAttributes()
		throws AppException
	{
		ChunkReader reader = new ChunkReader(READ_ATTRIBUTES, null);
		new IffFormFile(file).read(reader);
		return attributes;
	}

	//------------------------------------------------------------------

	public PrivateData readPrivateData()
		throws AppException
	{
		ChunkReader reader = new ChunkReader(READ_PRIVATE_DATA, null);
		new IffFormFile(file).read(reader);
		return (privateData == null) ? null : new PrivateData(privateData);
	}

	//------------------------------------------------------------------

	public OndaFile.Attributes readAttributesAndPrivateData()
		throws AppException
	{
		ChunkReader reader = new ChunkReader(READ_ATTRIBUTES | READ_PRIVATE_DATA, null);
		new IffFormFile(file).read(reader);
		return attributes;
	}

	//------------------------------------------------------------------

	public OndaFile.Attributes readData(IByteDataOutputStream outStream)
		throws AppException
	{
		ChunkReader reader = new ChunkReader(READ_ATTRIBUTES | READ_DATA, outStream);
		new IffFormFile(file).read(reader);
		return attributes;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	File				file;
	private	OndaFile.Attributes	attributes;
	private	byte[]				privateData;

}

//----------------------------------------------------------------------
