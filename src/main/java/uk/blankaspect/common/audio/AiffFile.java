/*====================================================================*\

AiffFile.java

Class: AIFF audio file.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.audio;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.bytedata.IByteDataInputStream;
import uk.blankaspect.common.bytedata.IByteDataOutputStream;
import uk.blankaspect.common.bytedata.IByteDataSource;

import uk.blankaspect.common.doubledata.IDoubleDataInputStream;
import uk.blankaspect.common.doubledata.IDoubleDataOutputStream;
import uk.blankaspect.common.doubledata.IDoubleDataSource;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.iff.Chunk;
import uk.blankaspect.common.iff.ChunkFilter;
import uk.blankaspect.common.iff.FormFile;
import uk.blankaspect.common.iff.Group;
import uk.blankaspect.common.iff.IffChunk;
import uk.blankaspect.common.iff.IffException;
import uk.blankaspect.common.iff.IffFormFile;
import uk.blankaspect.common.iff.IffGroup;
import uk.blankaspect.common.iff.IffId;

import uk.blankaspect.common.misc.IDataInput;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// CLASS: AIFF AUDIO FILE


public class AiffFile
	extends AudioFile
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		IffId	IFF_GROUP_ID	= new IffId("FORM");
	public static final		IffId	AIFF_TYPE_ID	= new IffId("AIFF");
	public static final		IffId	AIFF_COMMON_ID	= new IffId("COMM");
	public static final		IffId	AIFF_DATA_ID	= new IffId("SSND");

	private static final	int		SOUND_DATA_OFFSET_SIZE			= 4;
	private static final	int		SOUND_DATA_BLOCK_LENGTH_SIZE	= 4;
	private static final	int		SOUND_DATA_HEADER_SIZE			=
			SOUND_DATA_OFFSET_SIZE + SOUND_DATA_BLOCK_LENGTH_SIZE;

	private static final	IffChunk	COMMON_CHUNK	= new IffChunk(AIFF_COMMON_ID, null);
	private static final	IffChunk	DATA_CHUNK		= new IffChunk(AIFF_DATA_ID, null);

	private static final	String	FILE_IS_NOT_OPEN_STR	= "File is not open";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<IffChunk>	chunks;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public AiffFile(File file)
	{
		super(file);
		chunks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public AiffFile(File file,
					int  numChannels,
					int  bitsPerSample,
					int  sampleRate)
	{
		super(file, numChannels, bitsPerSample, sampleRate);
		chunks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public AiffFile(File     file,
					AiffFile aiffFile)
	{
		this(file, aiffFile.numChannels, aiffFile.bitsPerSample, aiffFile.sampleRate);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int setSampleData16(double[] data,
									  int      srcOffset,
									  byte[]   buffer,
									  int      destOffset,
									  int      length)
	{
		double maxSampleValue = getMaxOutputSampleValue(2);
		int endOffset = srcOffset + length;
		for (int i = srcOffset; i < endOffset; i++)
		{
			int sampleValue = (int)Math.round(data[i] * maxSampleValue);
			buffer[destOffset++] = (byte)(sampleValue >> 8);
			buffer[destOffset++] = (byte)sampleValue;
		}
		return destOffset;
	}

	//------------------------------------------------------------------

	public static int setSampleData24(double[] data,
									  int      srcOffset,
									  byte[]   buffer,
									  int      destOffset,
									  int      length)
	{
		double maxSampleValue = getMaxOutputSampleValue(3);
		int endOffset = srcOffset + length;
		for (int i = srcOffset; i < endOffset; i++)
		{
			int sampleValue = (int)Math.round(data[i] * maxSampleValue);
			buffer[destOffset++] = (byte)(sampleValue >> 16);
			buffer[destOffset++] = (byte)(sampleValue >> 8);
			buffer[destOffset++] = (byte)sampleValue;
		}
		return destOffset;
	}

	//------------------------------------------------------------------

	private static byte[] reverseByteOrder(byte[] data,
										   int    offset,
										   int    length,
										   int    bytesPerSample)
	{
		byte temp = 0;
		int endOffset = offset + length;
		switch (bytesPerSample)
		{
			case 2:
				while (offset < endOffset)
				{
					temp = data[offset];
					data[offset] = data[offset + 1];
					data[offset + 1] = temp;
					offset += bytesPerSample;
				}
				break;

			case 3:
				while (offset < endOffset)
				{
					temp = data[offset];
					data[offset] = data[offset + 2];
					data[offset + 2] = temp;
					offset += bytesPerSample;
				}
				break;

			case 4:
				while (offset < endOffset)
				{
					temp = data[offset];
					data[offset] = data[offset + 3];
					data[offset + 3] = temp;
					temp = data[offset + 1];
					data[offset + 1] = data[offset + 2];
					data[offset + 2] = temp;
					offset += bytesPerSample;
				}
				break;
		}
		return data;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public void addChunks(List<Chunk> chunks)
		throws ClassCastException
	{
		for (Chunk chunk : chunks)
			this.chunks.add((IffChunk)chunk);
	}

	//------------------------------------------------------------------

	@Override
	public void read(FormFile.IChunkReader chunkReader)
		throws AppException
	{
		new IffFormFile(file).read(chunkReader);
	}

	//------------------------------------------------------------------

	@Override
	public int read(double[] buffer,
					int      offset,
					int      length)
		throws AppException
	{
		// Test whether random access file is open
		if (raFile == null)
			throw new IllegalStateException(FILE_IS_NOT_OPEN_STR);

		// Read from random access file
		try
		{
			// Read from file
			int bytesPerSample = getBytesPerSample();
			byte[] inBuffer = new byte[length * bytesPerSample];
			int readLength = raFile.read(inBuffer);

			// Convert sample data
			if (readLength > 0)
			{
				double factor = 1.0 / getMaxInputSampleValue(bytesPerSample);
				if (bytesPerSample == 1)
				{
					for (int i = 0; i < readLength; i++)
						buffer[offset++] = (double)(inBuffer[i] ^ (byte)0x80) * factor;
				}
				else
				{
					for (int i = 0; i < readLength; i += bytesPerSample)
						buffer[offset++] = (double)NumberCodec.bytesToIntBE(inBuffer, i, bytesPerSample) * factor;
					readLength /= bytesPerSample;
				}
			}
			return readLength;
		}
		catch (IOException e)
		{
			throw new FileException(AudioFile.ErrorId.ERROR_READING_FILE, file, e);
		}
	}

	//------------------------------------------------------------------

	@Override
	protected Object read(SampleFormat sampleFormat,
						  int          bytesPerSample,
						  Object       outStream,
						  ChunkFilter  filter)
		throws AppException
	{
		ChunkReader reader = new ChunkReader(sampleFormat, bytesPerSample, outStream, filter);
		new IffFormFile(file).read(reader);
		return reader.getData();
	}

	//------------------------------------------------------------------

	@Override
	protected int readGroupHeader()
		throws AppException
	{
		try
		{
			// Read group header
			byte[] buffer = new byte[Group.HEADER_SIZE];
			raFile.readFully(buffer);

			// Test for AIFF group
			IffGroup group = new IffGroup(buffer);
			if (!group.getGroupId().equals(IFF_GROUP_ID) ||
				 !group.getTypeId().equals(AIFF_TYPE_ID))
				throw new FileException(ErrorId.NOT_AN_AIFF_FILE, file);

			// Test group size
			int groupSize = IffChunk.getSize(buffer, IffId.SIZE);
			if (groupSize < 0)
				throw new FileException(ErrorId.FILE_IS_TOO_LARGE, file);
			if (groupSize > raFile.length() - Chunk.HEADER_SIZE)
				throw new FileException(AudioFile.ErrorId.MALFORMED_FILE, file);

			return groupSize;
		}
		catch (IOException e)
		{
			throw new FileException(AudioFile.ErrorId.ERROR_READING_FILE, file, e);
		}
	}

	//------------------------------------------------------------------

	@Override
	protected void write(IDataInput      sampleDataInput,
						 IDataInput.Kind inputKind)
		throws AppException
	{
		// Create placeholders for required chunks
		if (chunks.isEmpty())
		{
			chunks.add(COMMON_CHUNK);
			chunks.add(DATA_CHUNK);
		}

		// Write file
		new IffFormFile(file).write(AIFF_TYPE_ID, new ChunkWriter(sampleDataInput, inputKind));
	}

	//------------------------------------------------------------------

	@Override
	protected int getChunkSize(byte[] buffer,
							   int    offset)
	{
		return IffChunk.getSize(buffer, offset);
	}

	//------------------------------------------------------------------

	@Override
	protected IffId getDataChunkId()
	{
		return AIFF_DATA_ID;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public IffChunk getChunk(IffId id)
	{
		for (IffChunk chunk : chunks)
		{
			if (chunk.getId().equals(id))
				return chunk;
		}
		return null;
	}

	//------------------------------------------------------------------

	public void setChunks(List<IffChunk> chunks)
	{
		this.chunks = chunks;
	}

	//------------------------------------------------------------------

	public IffChunk readChunk(IffId id)
		throws AppException
	{
		// Test whether random access file is open
		if (raFile == null)
			throw new IllegalStateException(FILE_IS_NOT_OPEN_STR);

		// Find chunk
		int chunkSize = findChunk(id);
		if (chunkSize < 0)
			return null;

		// Read chunk
		try
		{
			byte[] buffer = new byte[chunkSize];
			raFile.readFully(buffer);
			return new IffChunk(id, buffer);
		}
		catch (OutOfMemoryError e)
		{
			throw new IffException(ErrorId.NOT_ENOUGH_MEMORY, file, id);
		}
		catch (IOException e)
		{
			throw new FileException(AudioFile.ErrorId.ERROR_READING_FILE, file, e);
		}
	}

	//------------------------------------------------------------------

	private void parseCommonChunk(RandomAccessFile raFile,
								  int              chunkSize)
		throws AppException, IOException
	{
		if ((chunkSize != Attributes.CHUNK_SIZE1) && (chunkSize < Attributes.CHUNK_SIZE2))
			throw new FileException(ErrorId.INVALID_COMMON_CHUNK, file);

		byte[] buffer = new byte[chunkSize];
		raFile.readFully(buffer);

		Attributes attributes = null;
		try
		{
			attributes = new Attributes(buffer);
		}
		catch (IllegalArgumentException e)
		{
			throw new FileException(ErrorId.INVALID_COMMON_CHUNK, file);
		}
		if (attributes.compressed)
			throw new IffException(ErrorId.UNSUPPORTED_FORMAT, file, AIFF_COMMON_ID);
		numChannels = attributes.numChannels;
		bitsPerSample = attributes.bitsPerSample;
		if (bitsPerSample > MAX_BITS_PER_SAMPLE)
			throw new FileException(ErrorId.UNSUPPORTED_BITS_PER_SAMPLE, file);
		numSampleFrames = attributes.numSampleFrames;
		sampleRate = attributes.sampleRate;
		if (sampleRate == 0)
			throw new FileException(ErrorId.SAMPLE_RATE_OUT_OF_BOUNDS, file);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: ERROR IDENTIFIERS


	private enum ErrorId
		implements AppException.IId
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		FAILED_TO_READ_SAMPLE_DATA
		("Failed to read data from the sample data input stream."),

		NOT_AN_AIFF_FILE
		("The file is not a valid AIFF file."),

		UNSUPPORTED_FORMAT
		("This program cannot process AIFF files in compressed format."),

		UNSUPPORTED_BITS_PER_SAMPLE
		("This program cannot process AIFF files with a resolution of more than 32 bits per sample."),

		SAMPLE_RATE_OUT_OF_BOUNDS
		("The sample rate is out of bounds."),

		NO_COMMON_CHUNK
		("The file does not have a Common chunk."),

		NO_COMMON_CHUNK_BEFORE_DATA_CHUNK
		("There is no Common chunk before the data chunk."),

		MULTIPLE_COMMON_CHUNKS
		("The file has more than one Common chunk."),

		INVALID_COMMON_CHUNK
		("The Common chunk is invalid."),

		MULTIPLE_DATA_CHUNKS
		("The file has more than one data chunk."),

		INVALID_DATA_CHUNK
		("The data chunk is invalid."),

		INCONSISTENT_DATA_SIZE
		("The size of the data chunk is inconsistent with the number of sample frames."),

		FILE_IS_TOO_LARGE
		("This program cannot process AIFF files that are larger than 2GB."),

		NOT_ENOUGH_MEMORY
		("There was not enough memory to read the file.");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	message;

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

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: ATTRIBUTES


	private static class Attributes
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		private static final	int		NUM_CHANNELS_SIZE			= 2;
		private static final	int		NUM_SAMPLE_FRAMES_SIZE		= 4;
		private static final	int		BITS_PER_SAMPLE_SIZE		= 2;
		private static final	int		SAMPLE_RATE_EXPONENT_SIZE	= 2;
		private static final	int		SAMPLE_RATE_MANTISSA_SIZE	= 8;
		private static final	int		SAMPLE_RATE_SIZE			=
				SAMPLE_RATE_EXPONENT_SIZE +SAMPLE_RATE_MANTISSA_SIZE;

		private static final	int		CHUNK_SIZE1	=
				NUM_CHANNELS_SIZE + NUM_SAMPLE_FRAMES_SIZE + BITS_PER_SAMPLE_SIZE + SAMPLE_RATE_SIZE;
		private static final	int		CHUNK_SIZE2	= CHUNK_SIZE1 + IffId.SIZE;

		private static final	int		SAMPLE_RATE_EXPONENT_BIAS	= 16383;

		private static final	IffId	NONE_ID	= new IffId("NONE");

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		int		numChannels;
		int		numSampleFrames;
		int		bitsPerSample;
		int		sampleRate;
		boolean	compressed;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Attributes(int numChannels,
						   int bitsPerSample,
						   int numSampleFrames,
						   int sampleRate)
		{
			this.numChannels = numChannels;
			this.numSampleFrames = numSampleFrames;
			this.bitsPerSample = bitsPerSample;
			this.sampleRate = sampleRate;
		}

		//--------------------------------------------------------------

		private Attributes(byte[] data)
		{
			set(data);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * @throws IllegalArgumentException
		 */

		public void set(byte[] data)
		{
			int offset = 0;
			numChannels = NumberCodec.bytesToUIntBE(data, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;

			numSampleFrames = NumberCodec.bytesToUIntBE(data, offset, NUM_SAMPLE_FRAMES_SIZE);
			offset += NUM_SAMPLE_FRAMES_SIZE;

			bitsPerSample = NumberCodec.bytesToUIntBE(data, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;

			int exponent = NumberCodec.bytesToIntBE(data, offset, SAMPLE_RATE_EXPONENT_SIZE);
			offset += SAMPLE_RATE_EXPONENT_SIZE;
			long mantissa = NumberCodec.bytesToULongBE(data, offset, SAMPLE_RATE_MANTISSA_SIZE);
			offset += SAMPLE_RATE_MANTISSA_SIZE;
			if (exponent > 0)
			{
				exponent -= SAMPLE_RATE_EXPONENT_BIAS;
				if (exponent <= 62)
				{
					mantissa >>>= 62 - exponent;
					long m2 = mantissa;
					mantissa >>>= 1;
					if ((m2 & 1) != 0)
						++mantissa;
					if (mantissa <= Integer.MAX_VALUE)
						sampleRate = (int)mantissa;
				}
			}

			if (data.length >= CHUNK_SIZE2)
				compressed = new IffId(data, offset).equals(NONE_ID);
		}

		//--------------------------------------------------------------

		public byte[] get()
		{
			byte[] buffer = new byte[CHUNK_SIZE1];
			put(buffer, 0);
			return buffer;
		}

		//--------------------------------------------------------------

		public int put(byte[] buffer,
					   int    offset)
		{
			NumberCodec.uIntToBytesBE(numChannels, buffer, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;

			NumberCodec.uIntToBytesBE(numSampleFrames, buffer, offset, NUM_SAMPLE_FRAMES_SIZE);
			offset += NUM_SAMPLE_FRAMES_SIZE;

			NumberCodec.uIntToBytesBE(bitsPerSample, buffer, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;

			int exponent = SAMPLE_RATE_EXPONENT_BIAS - 33;
			long mantissa = sampleRate & 0xFFFFFFFFL;
			while (mantissa >= 0)
			{
				mantissa <<= 1;
				++exponent;
			}
			NumberCodec.intToBytesBE(exponent, buffer, offset, SAMPLE_RATE_EXPONENT_SIZE);
			offset += SAMPLE_RATE_EXPONENT_SIZE;
			NumberCodec.uLongToBytesBE(mantissa, buffer, offset, SAMPLE_RATE_MANTISSA_SIZE);
			offset += SAMPLE_RATE_MANTISSA_SIZE;

			return offset;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: CHUNK READER


	private class ChunkReader
		implements FormFile.IChunkReader
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	SampleFormat	sampleFormat;
		private	int				bytesPerSample;
		private	Object			outStream;
		private	ChunkFilter		filter;
		private	Object			sampleData;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ChunkReader(SampleFormat sampleFormat,
							int          bytesPerSample,
							Object       outStream,
							ChunkFilter  filter)
		{
			this.sampleFormat = sampleFormat;
			this.bytesPerSample = bytesPerSample;
			this.outStream = outStream;
			this.filter = filter;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FormFile.IChunkReader interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void beginReading(RandomAccessFile raFile,
								 IffId            typeId,
								 int              size)
			throws AppException
		{
			if (size < 0)
				throw new FileException(ErrorId.FILE_IS_TOO_LARGE, file);
			if (!typeId.equals(AIFF_TYPE_ID))
				throw new FileException(ErrorId.NOT_AN_AIFF_FILE, file);
			chunks.clear();
		}

		//--------------------------------------------------------------

		@Override
		public void read(RandomAccessFile raFile,
						 IffId            id,
						 int              size)
			throws AppException, IOException
		{

			//----  Common chunk

			if (id.equals(AIFF_COMMON_ID))
			{
				if (chunks.contains(COMMON_CHUNK))
					throw new FileException(ErrorId.MULTIPLE_COMMON_CHUNKS, file);

				parseCommonChunk(raFile, size);
				chunks.add(COMMON_CHUNK);
			}


			//----  Data chunk

			else if (id.equals(AIFF_DATA_ID))
			{
				// Test for chunks
				if (!chunks.contains(COMMON_CHUNK))
					throw new FileException(ErrorId.NO_COMMON_CHUNK_BEFORE_DATA_CHUNK, file);

				if (chunks.contains(DATA_CHUNK))
					throw new FileException(ErrorId.MULTIPLE_DATA_CHUNKS, file);

				// Skip padding before sample data
				byte[] buffer = new byte[SOUND_DATA_HEADER_SIZE];
				raFile.readFully(buffer);
				int offset = NumberCodec.bytesToIntBE(buffer, 0, SOUND_DATA_OFFSET_SIZE);
				if (offset < 0)
					throw new FileException(ErrorId.INVALID_DATA_CHUNK, file);
				raFile.skipBytes(offset);

				// Read and parse sample data
				int dataSize = numSampleFrames * getBytesPerSampleFrame();
				if (size < SOUND_DATA_HEADER_SIZE + offset + dataSize)
					throw new FileException(ErrorId.INCONSISTENT_DATA_SIZE, file);

				try
				{
					switch (sampleFormat)
					{
						case INTEGER:
							sampleData = parseIntegerData(raFile, dataSize, bytesPerSample,
														  (IByteDataOutputStream)outStream);
							break;

						case DOUBLE:
							sampleData = parseDoubleData(raFile, dataSize, bytesPerSample,
														 (IDoubleDataOutputStream)outStream);
							break;

						case NONE:
							// do nothing
							break;
					}
				}
				catch (OutOfMemoryError e)
				{
					throw new IffException(ErrorId.NOT_ENOUGH_MEMORY, file, id);
				}
				chunks.add(DATA_CHUNK);
			}


			//----  Ancillary chunk

			else if ((filter != null) && filter.accept(id) && (sampleFormat != SampleFormat.NONE))
			{
				try
				{
					byte[] buffer = new byte[size];
					raFile.readFully(buffer);
					chunks.add(new IffChunk(id, buffer));
				}
				catch (OutOfMemoryError e)
				{
					throw new IffException(ErrorId.NOT_ENOUGH_MEMORY, file, id);
				}
			}
		}

		//--------------------------------------------------------------

		@Override
		public void endReading(RandomAccessFile raFile)
			throws AppException
		{
			// Test for critical chunks
			if (!chunks.contains(COMMON_CHUNK))
				throw new FileException(ErrorId.NO_COMMON_CHUNK, file);
			if (!chunks.contains(DATA_CHUNK))
				throw new FileException(AudioFile.ErrorId.NO_DATA_CHUNK, file);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public Object getData()
		{
			return sampleData;
		}

		//--------------------------------------------------------------

		private byte[] parseIntegerData(RandomAccessFile      raFile,
										int                   dataSize,
										int                   bytesPerSample,
										IByteDataOutputStream outStream)
			throws AppException, IOException
		{
			final	int	BUFFER_LENGTH	= 1 << 12;  // 4096

			// Initialise variables
			byte[] outBuffer = null;
			int inBytesPerSample = getBytesPerSample();

			// If output sample size is equal to input sample size, read sample data from file, reverse
			// their byte order and write them to buffer or output stream ...
			if ((bytesPerSample == 0) || (bytesPerSample == inBytesPerSample))
			{
				if (outStream == null)
				{
					outBuffer = new byte[dataSize];
					raFile.readFully(outBuffer);
					reverseByteOrder(outBuffer, 0, outBuffer.length, inBytesPerSample);
				}
				else
				{
					int bufferLength = BUFFER_LENGTH * inBytesPerSample;
					outBuffer = new byte[bufferLength];
					int lengthRemaining = dataSize;
					while (lengthRemaining > 0)
					{
						int inLength = bufferLength;
						if (inLength > lengthRemaining)
							inLength = lengthRemaining;
						raFile.readFully(outBuffer, 0, inLength);
						reverseByteOrder(outBuffer, 0, inLength, inBytesPerSample);
						outStream.write(outBuffer, 0, inLength);
						lengthRemaining -= inLength;
					}
				}
			}

			// ... otherwise, read sample data from file, convert them, and write them to buffer or output
			// stream
			else
			{
				int delta = bytesPerSample - inBytesPerSample;
				double factor = (delta < 0) ? getMaxInputSampleValue(bytesPerSample) /
																getMaxInputSampleValue(inBytesPerSample)
											: 0.0;
				int inBufferLength = BUFFER_LENGTH * inBytesPerSample;
				byte[] inBuffer = new byte[inBufferLength];
				outBuffer = new byte[((outStream == null) ? dataSize / inBytesPerSample : BUFFER_LENGTH) *
																							bytesPerSample];
				int outIndex = 0;
				int lengthRemaining = dataSize;
				while (lengthRemaining > 0)
				{
					int inLength = inBufferLength;
					if (inLength > lengthRemaining)
						inLength = lengthRemaining;
					raFile.readFully(inBuffer, 0, inLength);
					if (delta > 0)
					{
						int inIndex = 0;
						if (inBytesPerSample == 1)
						{
							while (inIndex < inLength)
							{
								for (int i = 0; i < delta; i++)
									outBuffer[outIndex++] = 0;
								outBuffer[outIndex++] = (byte)(inBuffer[inIndex++] ^ 0x80);
							}
						}
						else
						{
							while (inIndex < inLength)
							{
								for (int i = 0; i < delta; i++)
									outBuffer[outIndex++] = 0;
								for (int i = 0; i < inBytesPerSample; i++)
									outBuffer[outIndex++] = inBuffer[inIndex++];
							}
						}
					}
					else
					{
						int inIndex = 0;
						while (inIndex < inLength)
						{
							int value = (int)Math.round((double)NumberCodec.bytesToIntBE(inBuffer, inIndex,
																						 inBytesPerSample) * factor);
							NumberCodec.intToBytesBE(value, outBuffer, outIndex, bytesPerSample);
							inIndex += inBytesPerSample;
							outIndex += bytesPerSample;
						}
					}
					if (outStream != null)
					{
						outStream.write(outBuffer, 0, outIndex);
						outIndex = 0;
					}
					lengthRemaining -= inLength;
				}
			}

			// If sample data were written to buffer, return it
			return ((outStream == null) ? outBuffer : null);
		}

		//--------------------------------------------------------------

		private double[] parseDoubleData(RandomAccessFile        raFile,
										 int                     dataSize,
										 int                     bytesPerSample,
										 IDoubleDataOutputStream outStream)
			throws AppException, IOException
		{
			final	int	BUFFER_LENGTH	= 1 << 12;  // 4096

			// Initialise variables
			int inBytesPerSample = getBytesPerSample();
			double factor = 1.0 / getMaxInputSampleValue(inBytesPerSample);
			int inBufferLength = BUFFER_LENGTH * inBytesPerSample;
			byte[] inBuffer = new byte[inBufferLength];
			double[] outBuffer = new double[(outStream == null) ? dataSize / inBytesPerSample : BUFFER_LENGTH];
			int outIndex = 0;
			int lengthRemaining = dataSize;

			// Read sample data from file and write them to buffer or output stream
			while (lengthRemaining > 0)
			{
				// Read sample data from file
				int inLength = inBufferLength;
				if (inLength > lengthRemaining)
					inLength = lengthRemaining;
				raFile.readFully(inBuffer, 0, inLength);

				// Convert sample values to floating point
				if (inBytesPerSample == 1)
				{
					for (int i = 0; i < inLength; i++)
						outBuffer[outIndex++] = (double)(inBuffer[i] ^ (byte)0x80) * factor;
				}
				else
				{
					for (int i = 0; i < inLength; i += inBytesPerSample)
					{
						outBuffer[outIndex++] =
								(double)NumberCodec.bytesToIntBE(inBuffer, i, inBytesPerSample) * factor;
					}
				}

				// Write data to output stream
				if (outStream != null)
				{
					outStream.write(outBuffer, 0, outIndex);
					outIndex = 0;
				}

				// Decrement length of sample data that remain to be read
				lengthRemaining -= inLength;
			}

			// If sample data were written to buffer, return it
			return (outStream == null) ? outBuffer : null;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// CLASS: CHUNK WRITER


	private class ChunkWriter
		implements FormFile.IChunkWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	IDataInput		sampleDataInput;
		private	IDataInput.Kind	inputKind;
		private	int				chunkIndex;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private ChunkWriter(IDataInput      sampleDataInput,
							IDataInput.Kind inputKind)
		{
			this.sampleDataInput = sampleDataInput;
			this.inputKind = inputKind;
			numSampleFrames = (int)(getDataLength() / getBytesPerSampleFrame());
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : FormFile.IChunkWriter interface
	////////////////////////////////////////////////////////////////////

		@Override
		public void beginWriting(RandomAccessFile raFile)
		{
			// do nothing
		}

		//--------------------------------------------------------------

		@Override
		public IffId getNextId()
		{
			return ((chunkIndex < chunks.size()) ? chunks.get(chunkIndex++).getId() : null);
		}

		//--------------------------------------------------------------

		@Override
		public void write(RandomAccessFile raFile,
						  IffId            id)
			throws AppException, IOException
		{
			// Common chunk
			if (id.equals(AIFF_COMMON_ID))
				raFile.write(new Attributes(numChannels, bitsPerSample, numSampleFrames, sampleRate).get());

			// Data chunk
			else if (id.equals(AIFF_DATA_ID))
				writeData(raFile);

			// Ancillary chunk
			else
			{
				IffChunk chunk = getChunk(id);
				if (chunk != null)
					raFile.write(chunk.getData());
			}
		}

		//--------------------------------------------------------------

		@Override
		public void endWriting(RandomAccessFile raFile)
		{
			// do nothing
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		private long getDataLength()
		{
			long length = sampleDataInput.getLength();
			if (inputKind.isDoubleInput())
			{
				switch (bitsPerSample)
				{
					case 16:
						length *= 2;
						break;

					case 24:
						length *= 3;
						break;
				}
			}
			return length;
		}

		//--------------------------------------------------------------

		private void writeData(DataOutput dataOutput)
			throws AppException, IOException
		{
			final	int	BYTE_BUFFER_LENGTH		= 1 << 13;  // 8192
			final	int	DOUBLE_BUFFER_LENGTH	= BYTE_BUFFER_LENGTH >> (Double.SIZE >> 3);

			// Reset data input
			sampleDataInput.reset();

			// Get length of input data
			long dataLength = getDataLength();

			// Write zero offset and block length
			dataOutput.write(new byte[SOUND_DATA_HEADER_SIZE]);

			// Write data
			switch (inputKind)
			{
				case BYTE_STREAM:
				{
					int bytesPerSample = getBytesPerSample();
					byte[] buffer = new byte[BYTE_BUFFER_LENGTH];
					int blockLength = 0;
					for (long offset = 0; offset < dataLength; offset += blockLength)
					{
						blockLength = (int)Math.min(dataLength - offset, buffer.length);
						blockLength = ((IByteDataInputStream)sampleDataInput).read(buffer, 0, blockLength);
						if (blockLength < 0)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
						reverseByteOrder(buffer, 0, blockLength, bytesPerSample);
						dataOutput.write(buffer, 0, blockLength);
					}
					break;
				}

				case BYTE_SOURCE:
				{
					int bytesPerSample = getBytesPerSample();
					long offset = 0;
					while (offset < dataLength)
					{
						IByteDataSource.ByteData data = ((IByteDataSource)sampleDataInput).getData();
						if (data == null)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
						reverseByteOrder(data.data, data.offset, data.length, bytesPerSample);
						dataOutput.write(data.data, data.offset, data.length);
						offset += data.length;
					}
					break;
				}

				case DOUBLE_STREAM:
				{
					double[] inBuffer = new double[DOUBLE_BUFFER_LENGTH];
					byte[] outBuffer = new byte[BYTE_BUFFER_LENGTH];
					int blockLength = 0;
					for (long offset = 0; offset < dataLength; offset += blockLength)
					{
						blockLength = (int)Math.min(dataLength - offset, inBuffer.length);
						blockLength = ((IDoubleDataInputStream)sampleDataInput).read(inBuffer, 0, blockLength);
						if (blockLength < 0)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
						switch (bitsPerSample)
						{
							case 16:
								setSampleData16(inBuffer, 0, outBuffer, 0, blockLength);
								blockLength *= 2;
								break;

							case 24:
								setSampleData24(inBuffer, 0, outBuffer, 0, blockLength);
								blockLength *= 3;
								break;
						}
						dataOutput.write(outBuffer, 0, blockLength);
					}
					break;
				}

				case DOUBLE_SOURCE:
				{
					long offset = 0;
					while (offset < dataLength)
					{
						IDoubleDataSource.DoubleData data = ((IDoubleDataSource)sampleDataInput).getData();
						if (data == null)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
						byte[] buffer = null;
						switch (bitsPerSample)
						{
							case 16:
								buffer = new byte[data.length * 2];
								setSampleData16(data.data, data.offset, buffer, 0, data.length);
								break;

							case 24:
								buffer = new byte[data.length * 3];
								setSampleData24(data.data, data.offset, buffer, 0, data.length);
								break;
						}
						dataOutput.write(buffer);
						offset += buffer.length;
					}
					break;
				}
			}
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
