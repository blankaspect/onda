/*====================================================================*\

WaveFile.java

Class: WAVE audio file.

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
import uk.blankaspect.common.iff.IffException;
import uk.blankaspect.common.iff.IffId;
import uk.blankaspect.common.iff.RiffChunk;
import uk.blankaspect.common.iff.RiffFormFile;
import uk.blankaspect.common.iff.RiffGroup;

import uk.blankaspect.common.misc.IDataInput;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// CLASS: WAVE AUDIO FILE


public class WaveFile
	extends AudioFile
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		IffId	RIFF_GROUP_ID	= new IffId("RIFF");
	public static final		IffId	WAVE_TYPE_ID	= new IffId("WAVE");
	public static final		IffId	WAVE_FORMAT_ID	= new IffId("fmt ");
	public static final		IffId	WAVE_DATA_ID	= new IffId("data");

	private static final	RiffChunk	FORMAT_CHUNK	= new RiffChunk(WAVE_FORMAT_ID, null);
	private static final	RiffChunk	DATA_CHUNK		= new RiffChunk(WAVE_DATA_ID, null);

	private static final	String	FILE_IS_NOT_OPEN_STR	= "File is not open";

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public WaveFile(File file)
	{
		super(file);
		chunks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public WaveFile(File file,
					int  numChannels,
					int  bitsPerSample,
					int  sampleRate)
	{
		super(file, numChannels, bitsPerSample, sampleRate);
		chunks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public WaveFile(File     file,
					WaveFile waveFile)
	{
		this(file, waveFile.numChannels, waveFile.bitsPerSample, waveFile.sampleRate);
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
			buffer[destOffset++] = (byte)sampleValue;
			buffer[destOffset++] = (byte)(sampleValue >> 8);
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
			buffer[destOffset++] = (byte)sampleValue;
			buffer[destOffset++] = (byte)(sampleValue >> 8);
			buffer[destOffset++] = (byte)(sampleValue >> 16);
		}
		return destOffset;
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
			this.chunks.add((RiffChunk)chunk);
	}

	//------------------------------------------------------------------

	@Override
	public void read(FormFile.IChunkReader chunkReader)
		throws AppException
	{
		new RiffFormFile(file).read(chunkReader);
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
						buffer[offset++] = (double)NumberCodec.bytesToIntLE(inBuffer, i, bytesPerSample) * factor;
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
		new RiffFormFile(file).read(reader);
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

			// Test for WAVE group
			RiffGroup group = new RiffGroup(buffer);
			if (!group.getGroupId().equals(RIFF_GROUP_ID) ||
				 !group.getTypeId().equals(WAVE_TYPE_ID))
				throw new FileException(ErrorId.NOT_A_WAVE_FILE, file);

			// Test group size
			int groupSize = RiffChunk.getSize(buffer, IffId.SIZE);
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
			chunks.add(FORMAT_CHUNK);
			chunks.add(DATA_CHUNK);
		}

		// Write file
		new RiffFormFile(file).write(WAVE_TYPE_ID, new ChunkWriter(sampleDataInput, inputKind));
	}

	//------------------------------------------------------------------

	@Override
	protected int getChunkSize(byte[] buffer,
							   int    offset)
	{
		return RiffChunk.getSize(buffer, offset);
	}

	//------------------------------------------------------------------

	@Override
	protected IffId getDataChunkId()
	{
		return WAVE_DATA_ID;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public RiffChunk getChunk(IffId id)
	{
		for (RiffChunk chunk : chunks)
		{
			if (chunk.getId().equals(id))
				return chunk;
		}
		return null;
	}

	//------------------------------------------------------------------

	public void setChunks(List<RiffChunk> chunks)
	{
		this.chunks = chunks;
	}

	//------------------------------------------------------------------

	public RiffChunk readChunk(IffId id)
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
			return new RiffChunk(id, buffer);
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

	private void parseFormatChunk(RandomAccessFile raFile,
								  int              chunkSize)
		throws AppException, IOException
	{
		if ((chunkSize != Attributes.CHUNK_SIZE1) && (chunkSize < Attributes.CHUNK_SIZE2))
			throw new FileException(ErrorId.INVALID_FORMAT_CHUNK, file);

		byte[] buffer = new byte[chunkSize];
		raFile.readFully(buffer);

		Attributes attributes = new Attributes(buffer);
		if (attributes.kind != Attributes.UNCOMPRESSED)
			throw new IffException(ErrorId.UNSUPPORTED_FORMAT, file, WAVE_FORMAT_ID);
		numChannels = attributes.numChannels;
		bitsPerSample = attributes.bitsPerSample;
		if (bitsPerSample > MAX_BITS_PER_SAMPLE)
			throw new FileException(ErrorId.UNSUPPORTED_BITS_PER_SAMPLE, file);
		sampleRate = attributes.samplesPerSecond;
		if (attributes.bytesPerSecond != getBytesPerSampleFrame() * sampleRate)
			throw new FileException(ErrorId.INCONSISTENT_AVERAGE_BYTES_PER_SECOND, file);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<RiffChunk>	chunks;

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

		NOT_A_WAVE_FILE
		("The file is not a valid WAVE file."),

		UNSUPPORTED_FORMAT
		("This program cannot process WAVE files in compressed format."),

		UNSUPPORTED_BITS_PER_SAMPLE
		("This program cannot process WAVE files with a resolution of more than 32 bits per sample."),

		INCONSISTENT_AVERAGE_BYTES_PER_SECOND
		("The bytes-per-second value in the format chunk is not consistent with the sample size and " +
			"sample rate."),

		NO_FORMAT_CHUNK
		("The file does not have a Format chunk."),

		NO_FORMAT_CHUNK_BEFORE_DATA_CHUNK
		("There is no Format chunk before the data chunk."),

		MULTIPLE_FORMAT_CHUNKS
		("The file has more than one Format chunk."),

		INVALID_FORMAT_CHUNK
		("The Format chunk is invalid."),

		MULTIPLE_DATA_CHUNKS
		("The file has more than one data chunk."),

		INCONSISTENT_DATA_SIZE
		("The size of the data chunk is inconsistent with the data format."),

		FILE_IS_TOO_LARGE
		("This program cannot process WAVE files that are larger than 2GB."),

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

		private static final	int	KIND_SIZE				= 2;
		private static final	int	NUM_CHANNELS_SIZE		= 2;
		private static final	int	SAMPLES_PER_SECOND_SIZE	= 4;
		private static final	int	BYTES_PER_SECOND_SIZE	= 4;
		private static final	int	BLOCK_ALIGN_SIZE		= 2;
		private static final	int	BITS_PER_SAMPLE_SIZE	= 2;

		private static final	int	CHUNK_SIZE1	=
				KIND_SIZE + NUM_CHANNELS_SIZE + SAMPLES_PER_SECOND_SIZE + BYTES_PER_SECOND_SIZE + BLOCK_ALIGN_SIZE
				+ BITS_PER_SAMPLE_SIZE;
		private static final	int	CHUNK_SIZE2	= CHUNK_SIZE1 + 2;

		private static final	int	UNCOMPRESSED	= 1;

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		int	kind;
		int	numChannels;
		int	samplesPerSecond;
		int	bytesPerSecond;
		int	blockAlign;
		int	bitsPerSample;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Attributes(int numChannels,
						   int bitsPerSample,
						   int sampleRate)
		{
			kind = UNCOMPRESSED;
			this.numChannels = numChannels;
			samplesPerSecond = sampleRate;
			int bytesPerSampleFrame = numChannels * bitsPerSampleToBytesPerSample(bitsPerSample);
			bytesPerSecond = bytesPerSampleFrame * sampleRate;
			blockAlign = bytesPerSampleFrame;
			this.bitsPerSample = bitsPerSample;
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

		public void set(byte[] data)
		{
			int offset = 0;
			kind = NumberCodec.bytesToUIntLE(data, offset, KIND_SIZE);
			offset += KIND_SIZE;

			numChannels = NumberCodec.bytesToUIntLE(data, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;

			samplesPerSecond = NumberCodec.bytesToUIntLE(data, offset, SAMPLES_PER_SECOND_SIZE);
			offset += SAMPLES_PER_SECOND_SIZE;

			bytesPerSecond = NumberCodec.bytesToUIntLE(data, offset, BYTES_PER_SECOND_SIZE);
			offset += BYTES_PER_SECOND_SIZE;

			blockAlign = NumberCodec.bytesToUIntLE(data, offset, BLOCK_ALIGN_SIZE);
			offset += BLOCK_ALIGN_SIZE;

			bitsPerSample = NumberCodec.bytesToUIntLE(data, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;
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
			NumberCodec.uIntToBytesLE(kind, buffer, offset, KIND_SIZE);
			offset += KIND_SIZE;

			NumberCodec.uIntToBytesLE(numChannels, buffer, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;

			NumberCodec.uIntToBytesLE(samplesPerSecond, buffer, offset, SAMPLES_PER_SECOND_SIZE);
			offset += SAMPLES_PER_SECOND_SIZE;

			NumberCodec.uIntToBytesLE(bytesPerSecond, buffer, offset, BYTES_PER_SECOND_SIZE);
			offset += BYTES_PER_SECOND_SIZE;

			NumberCodec.uIntToBytesLE(blockAlign, buffer, offset, BLOCK_ALIGN_SIZE);
			offset += BLOCK_ALIGN_SIZE;

			NumberCodec.uIntToBytesLE(bitsPerSample, buffer, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;

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
			if (!typeId.equals(WAVE_TYPE_ID))
				throw new FileException(ErrorId.NOT_A_WAVE_FILE, file);
			chunks.clear();
		}

		//--------------------------------------------------------------

		@Override
		public void read(RandomAccessFile raFile,
						 IffId            id,
						 int              size)
			throws AppException, IOException
		{

			//----  Format chunk

			if (id.equals(WAVE_FORMAT_ID))
			{
				if (chunks.contains(FORMAT_CHUNK))
					throw new FileException(ErrorId.MULTIPLE_FORMAT_CHUNKS, file);

				parseFormatChunk(raFile, size);
				chunks.add(FORMAT_CHUNK);
			}


			//---- Data chunk

			else if (id.equals(WAVE_DATA_ID))
			{
				// Test for chunks
				if (!chunks.contains(FORMAT_CHUNK))
					throw new FileException(ErrorId.NO_FORMAT_CHUNK_BEFORE_DATA_CHUNK, file);

				if (chunks.contains(DATA_CHUNK))
					throw new FileException(ErrorId.MULTIPLE_DATA_CHUNKS, file);

				// Validate size of chunk
				if (size % getBytesPerSampleFrame() != 0)
					throw new FileException(ErrorId.INCONSISTENT_DATA_SIZE, file);
				numSampleFrames = size / getBytesPerSampleFrame();

				// Read and parse sample data
				try
				{
					switch (sampleFormat)
					{
						case INTEGER:
							sampleData = parseIntegerData(raFile, size, bytesPerSample,
														  (IByteDataOutputStream)outStream);
							break;

						case DOUBLE:
							sampleData = parseDoubleData(raFile, size, bytesPerSample,
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
					chunks.add(new RiffChunk(id, buffer));
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
			if (!chunks.contains(FORMAT_CHUNK))
				throw new FileException(ErrorId.NO_FORMAT_CHUNK, file);
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

			// If output sample size is equal to input sample size, read sample data from file and write
			// them to buffer or output stream without conversion ...
			if ((bytesPerSample == 0) || (bytesPerSample == inBytesPerSample))
			{
				if (outStream == null)
				{
					outBuffer = new byte[dataSize];
					raFile.readFully(outBuffer);
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
							int value = (int)Math.round((double)NumberCodec.bytesToIntLE(inBuffer, inIndex,
																						 inBytesPerSample) * factor);
							NumberCodec.intToBytesLE(value, outBuffer, outIndex, bytesPerSample);
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
								(double)NumberCodec.bytesToIntLE(inBuffer, i, inBytesPerSample) * factor;
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
			// Format chunk
			if (id.equals(WAVE_FORMAT_ID))
				raFile.write(new Attributes(numChannels, bitsPerSample, sampleRate).get());

			// Data chunk
			else if (id.equals(WAVE_DATA_ID))
				writeData(raFile);

			// Ancillary chunk
			else
			{
				RiffChunk chunk = getChunk(id);
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

			// Write data
			switch (inputKind)
			{
				case BYTE_STREAM:
				{
					byte[] buffer = new byte[BYTE_BUFFER_LENGTH];
					int blockLength = 0;
					for (long offset = 0; offset < dataLength; offset += blockLength)
					{
						blockLength = (int)Math.min(dataLength - offset, buffer.length);
						blockLength = ((IByteDataInputStream)sampleDataInput).read(buffer, 0, blockLength);
						if (blockLength < 0)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
						dataOutput.write(buffer, 0, blockLength);
					}
					break;
				}

				case BYTE_SOURCE:
				{
					long offset = 0;
					while (offset < dataLength)
					{
						IByteDataSource.ByteData data = ((IByteDataSource)sampleDataInput).getData();
						if (data == null)
							throw new AppException(ErrorId.FAILED_TO_READ_SAMPLE_DATA);
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
