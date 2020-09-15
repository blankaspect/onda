/*====================================================================*\

PrivateData.java

Private data class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.iff.Chunk;
import uk.blankaspect.common.iff.ChunkFilter;
import uk.blankaspect.common.iff.FormFile;
import uk.blankaspect.common.iff.IffId;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// PRIVATE DATA CLASS


class PrivateData
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	SOURCE_KIND_SIZE	= 2;
	private static final	int	ADLER32_SIZE		= 4;
	private static final	int	NUM_CHUNKS_SIZE		= 4;
	private static final	int	HEADER_SIZE			= SOURCE_KIND_SIZE + ADLER32_SIZE + NUM_CHUNKS_SIZE;

	private static final	int	BLOCK_SIZE	= 1 << 12;  // 4096

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

		MALFORMED_DATA
		("The private data is malformed."),

		INVALID_DATA
		("The private data is invalid."),

		UNRECOGNISED_SOURCE_FILE_KIND
		("The private data belong to an unrecognised kind of audio file."),

		INVALID_NUM_CHUNKS
		("The number of chunks specified in the private data is invalid."),

		INCORRECT_ADLER32
		("The checksum of the private data is incorrect.");

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

		@Override
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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// SOURCE CHUNK CLASS


	private static class SourceChunk
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private SourceChunk(IffId id,
							int   size)
		{
			this.id = id;
			this.size = size;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		IffId	id;
		int		size;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CHUNK READER CLASS


	private class Reader
		implements FormFile.IChunkReader
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Reader(ChunkFilter filter)
		{
			this.filter = filter;
			compressor = new Deflater(Deflater.BEST_COMPRESSION);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FormFile.IChunkReader interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void beginReading(RandomAccessFile raFile,
								 IffId            typeId,
								 int              size)
		{
			sourceChunks.clear();
			compressedDataBlocks.clear();
			outBuffer = new byte[BLOCK_SIZE];
			outOffset = 0;
			compressor.reset();
		}

		//--------------------------------------------------------------

		@Override
		public void read(RandomAccessFile raFile,
						 IffId            id,
						 int              size)
			throws AppException, IOException
		{
			if (Utils.indexOf(id, sourceKind.getCriticalIds()) < 0)
			{
				if (filter.accept(id))
				{
					sourceChunks.add(new SourceChunk(id, size));
					if (size > 0)
					{
						byte[] buffer = new byte[size];
						raFile.readFully(buffer);
						compressor.setInput(buffer);
						updateCompressedData();
					}
				}
			}
			else
				sourceChunks.add(new SourceChunk(id, 0));
		}

		//--------------------------------------------------------------

		@Override
		public void endReading(RandomAccessFile raFile)
		{
			compressor.finish();
			updateCompressedData();
			if (outOffset > 0)
			{
				byte[] buffer = new byte[outOffset];
				System.arraycopy(outBuffer, 0, buffer, 0, buffer.length);
				compressedDataBlocks.add(buffer);
			}
			adler32 = compressor.getAdler();
			compressor.end();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private void updateCompressedData()
		{
			while (true)
			{
				int length = compressor.deflate(outBuffer, outOffset, outBuffer.length - outOffset);
				if (length == 0)
					break;
				outOffset += length;
				if (outOffset >= outBuffer.length)
				{
					compressedDataBlocks.add(outBuffer);
					outBuffer = new byte[BLOCK_SIZE];
					outOffset = 0;
				}
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	ChunkFilter	filter;
		private	byte[]		outBuffer;
		private	int			outOffset;
		private	Deflater	compressor;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public PrivateData(AudioFileKind sourceKind)
	{
		this.sourceKind = sourceKind;
		sourceChunks = new ArrayList<>();
		compressedDataBlocks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public PrivateData(byte[] data)
		throws AppException
	{
		sourceChunks = new ArrayList<>();
		compressedDataBlocks = new ArrayList<>();
		set(data);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public AudioFileKind getSourceKind()
	{
		return sourceKind;
	}

	//------------------------------------------------------------------

	public int getNumChunks()
	{
		return sourceChunks.size();
	}

	//------------------------------------------------------------------

	public List<IffId> getAncillaryIds()
	{
		List<IffId> ids = new ArrayList<>();
		for (SourceChunk element : sourceChunks)
		{
			if (Utils.indexOf(element.id, sourceKind.getCriticalIds()) < 0)
				ids.add(element.id);
		}
		return ids;
	}

	//------------------------------------------------------------------

	public byte[] getCompressedData()
	{
		// Allocate buffer for data
		int length = HEADER_SIZE + sourceChunks.size() * Chunk.HEADER_SIZE;
		for (byte[] data : compressedDataBlocks)
			length += data.length;
		byte[] buffer = new byte[length];

		// Set header in buffer
		int offset = 0;
		NumberUtils.intToBytesBE(sourceKind.ordinal(), buffer, offset, SOURCE_KIND_SIZE);
		offset += SOURCE_KIND_SIZE;
		NumberUtils.intToBytesBE(adler32, buffer, offset, ADLER32_SIZE);
		offset += ADLER32_SIZE;
		NumberUtils.intToBytesBE(sourceChunks.size(), buffer, offset, NUM_CHUNKS_SIZE);
		offset += NUM_CHUNKS_SIZE;

		// Set list of chunks in buffer
		for (SourceChunk element : sourceChunks)
		{
			element.id.put(buffer, offset);
			offset += IffId.SIZE;
			NumberUtils.intToBytesBE(element.size, buffer, offset, Chunk.SIZE_SIZE);
			offset += Chunk.SIZE_SIZE;
		}

		// Set compressed chunk data in buffer
		for (byte[] data : compressedDataBlocks)
		{
			System.arraycopy(data, 0, buffer, offset, data.length);
			offset += data.length;
		}

		return buffer;
	}

	//------------------------------------------------------------------

	public Reader getReader(ChunkFilter filter)
	{
		return new Reader(filter);
	}

	//------------------------------------------------------------------

	public void setChunk(int   index,
						 Chunk chunk)
	{
		int offset = 0;
		for (int i = 0; i < index; i++)
			offset += sourceChunks.get(i).size;
		chunk.set(sourceChunks.get(index).id, decompressedData, offset, sourceChunks.get(index).size);
	}

	//------------------------------------------------------------------

	private void set(byte[] data)
		throws AppException
	{
		// Parse header
		if (data.length < HEADER_SIZE)
			throw new AppException(ErrorId.MALFORMED_DATA);

		int offset = 0;
		int sourceKindIndex = NumberUtils.bytesToIntBE(data, offset, SOURCE_KIND_SIZE);
		offset += SOURCE_KIND_SIZE;
		if ((sourceKindIndex < 0) || (sourceKindIndex >= AudioFileKind.values().length))
			throw new AppException(ErrorId.UNRECOGNISED_SOURCE_FILE_KIND);
		sourceKind = AudioFileKind.values()[sourceKindIndex];

		int adler32 = NumberUtils.bytesToIntBE(data, offset, ADLER32_SIZE);
		offset += ADLER32_SIZE;

		int numChunks = NumberUtils.bytesToIntBE(data, offset, NUM_CHUNKS_SIZE);
		offset += NUM_CHUNKS_SIZE;
		if (numChunks < 0)
			throw new AppException(ErrorId.INVALID_NUM_CHUNKS);

		if (data.length < HEADER_SIZE + numChunks * Chunk.HEADER_SIZE)
			throw new AppException(ErrorId.MALFORMED_DATA);

		// Parse list of chunks
		int length = 0;
		for (int i = 0; i < numChunks; i++)
		{
			IffId id = new IffId(data, offset);
			offset += IffId.SIZE;
			int size = NumberUtils.bytesToIntBE(data, offset, Chunk.SIZE_SIZE);
			offset += Chunk.SIZE_SIZE;
			sourceChunks.add(new SourceChunk(id, size));
			length += size;
		}

		// Decompress data
		decompressedData = new byte[length];
		Inflater decompressor = new Inflater();
		decompressor.setInput(data, offset, data.length - offset);
		try
		{
			length = decompressor.inflate(decompressedData);
		}
		catch (DataFormatException e)
		{
			throw new AppException(ErrorId.INVALID_DATA);
		}

		// Validate data
		if (!decompressor.finished() || (length < decompressedData.length))
			throw new AppException(ErrorId.INVALID_DATA);
		if (decompressor.getAdler() != adler32)
			throw new AppException(ErrorId.INCORRECT_ADLER32);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	AudioFileKind		sourceKind;
	private	int					adler32;
	private	List<SourceChunk>	sourceChunks;
	private	List<byte[]>		compressedDataBlocks;
	private	byte[]				decompressedData;

}

//----------------------------------------------------------------------
