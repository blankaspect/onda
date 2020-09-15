/*====================================================================*\

OndaFile.java

Onda lossless audio compression file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;

import uk.blankaspect.common.misc.IByteDataOutputStream;
import uk.blankaspect.common.misc.IByteDataSource;

import uk.blankaspect.common.nlf.Chunk;
import uk.blankaspect.common.nlf.ChunkList;
import uk.blankaspect.common.nlf.Document;
import uk.blankaspect.common.nlf.Id;
import uk.blankaspect.common.nlf.NlfException;

import uk.blankaspect.common.number.NumberUtils;

//----------------------------------------------------------------------


// ONDA LOSSLESS AUDIO COMPRESSION FILE CLASS


class OndaFile
	implements OndaFileReader
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int	MIN_NUM_CHANNELS	= 1;
	public static final		int	MAX_NUM_CHANNELS	= 128;

	public static final		int	MIN_BITS_PER_SAMPLE	= 1;
	public static final		int	MAX_BITS_PER_SAMPLE	= 32;

	public static final		int	MIN_SAMPLE_RATE	= 1;
	public static final		int	MAX_SAMPLE_RATE	= Integer.MAX_VALUE;

	public static final		long	MIN_NUM_SAMPLE_FRAMES	= 0;
	public static final		long	MAX_NUM_SAMPLE_FRAMES	= (1L << 62) - 1;

	public static final		int	MIN_KEY_LENGTH	= 1;
	public static final		int	MAX_KEY_LENGTH	= 5;

	public static final		int	MIN_BLOCK_LENGTH		= 1;
	public static final		int	MAX_BLOCK_LENGTH		= 1 << 16;
	public static final		int	DEFAULT_BLOCK_LENGTH	= 256;

	public static final		int	MIN_SUPPORTED_VERSION	= 0;
	public static final		int	MAX_SUPPORTED_VERSION	= 1;

	private static final	Id	ONDA_ID				= new Id("Onda");
	private static final	Id	ATTRIBUTES_ID		= new Id("attributes");
	private static final	Id	PRIVATE_DATA_ID		= new Id("privateData");
	@SuppressWarnings("unused")
	private static final	Id	DATA_BLOCK_SIZE_ID	= new Id("dataBlockSize");
	private static final	Id	DATA_ID				= new Id("data");

	private static final	int	READ_ATTRIBUTES		= 1 << 0;
	private static final	int	READ_PRIVATE_DATA	= 1 << 1;
	private static final	int	READ_DATA			= 1 << 2;

	private static final	String	NAMESPACE_NAME			= "http://ns.blankaspect.uk/onda-1";
	private static final	String	NAMESPACE_NAME_REGEX	= "http://ns\\.[a-z.]+/onda-1";

	private static final	String	OFFSET_STR	= "Offset = 0x";

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

		FAILED_TO_CLOSE_FILE
		("Failed to close the file."),

		ERROR_READING_FILE
		("An error occurred when reading the file."),

		MALFORMED_FILE
		("The file is malformed."),

		NOT_AN_ONDA_FILE
		("The file is not an Onda file."),

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

		NUM_CHANNELS_OUT_OF_BOUNDS
		("The number of channels is out of bounds."),

		BITS_PER_SAMPLE_OUT_OF_BOUNDS
		("The number of bits per sample is out of bounds."),

		SAMPLE_RATE_OUT_OF_BOUNDS
		("The sample rate is out of bounds."),

		NUM_SAMPLE_FRAMES_OUT_OF_BOUNDS
		("The number of sample frames is out of bounds."),

		KEY_LENGTH_OUT_OF_BOUNDS
		("The length of the number of encoding bits is out of bounds."),

		BLOCK_LENGTH_OUT_OF_BOUNDS
		("The block length is out of bounds."),

		PRIVATE_DATA_ARE_TOO_LARGE
		("The private data in the file are too large for this program."),

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
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// COMPRESSED DATA SOURCE INTERFACE


	interface CompressedDataSource
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		IByteDataSource.ByteData getData()
			throws AppException;

		//--------------------------------------------------------------

		long getCrc();

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// ONDA FILE ATTRIBUTES CLASS


	public static class Attributes
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		public static final	int	VERSION_SIZE			= 2;
		public static final	int	NUM_CHANNELS_SIZE		= 2;
		public static final	int	BITS_PER_SAMPLE_SIZE	= 2;
		public static final	int	SAMPLE_RATE_SIZE		= 4;
		public static final	int	NUM_SAMPLE_FRAMES_SIZE	= 8;
		public static final	int	CRC_SIZE				= 4;
		public static final	int	CRC_OFFSET				= VERSION_SIZE + NUM_CHANNELS_SIZE +
																BITS_PER_SAMPLE_SIZE + SAMPLE_RATE_SIZE +
																NUM_SAMPLE_FRAMES_SIZE;
		public static final	int	KEY_LENGTH_SIZE			= 2;
		public static final	int	BLOCK_LENGTH_SIZE		= 4;
		public static final	int	SIZE					= CRC_OFFSET + CRC_SIZE + KEY_LENGTH_SIZE +
																						BLOCK_LENGTH_SIZE;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public Attributes()
		{
		}

		//--------------------------------------------------------------

		public Attributes(int  version,
						  int  numChannels,
						  int  bitsPerSample,
						  int  sampleRate,
						  long numSampleFrames,
						  long crcValue,
						  int  keyLength,
						  int  blockLength)
		{
			this.version = version;
			this.numChannels = numChannels;
			this.bitsPerSample = bitsPerSample;
			this.sampleRate = sampleRate;
			this.numSampleFrames = numSampleFrames;
			this.crcValue = crcValue;
			this.keyLength = keyLength;
			this.blockLength = blockLength;
		}

		//--------------------------------------------------------------

		public Attributes(byte[] data,
						  int    offset)
			throws AppException
		{
			// Version number
			version = NumberUtils.bytesToIntBE(data, offset, VERSION_SIZE);
			offset += VERSION_SIZE;
			if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
				throw new AppException(ErrorId.UNSUPPORTED_VERSION, Integer.toString(version));

			// Number of channels
			numChannels = NumberUtils.bytesToIntBE(data, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;
			if ((numChannels < MIN_NUM_CHANNELS) || (numChannels > MAX_NUM_CHANNELS))
				throw new AppException(ErrorId.NUM_CHANNELS_OUT_OF_BOUNDS);

			// Bits per sample
			bitsPerSample = NumberUtils.bytesToIntBE(data, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;
			if ((bitsPerSample < MIN_BITS_PER_SAMPLE) || (bitsPerSample > MAX_BITS_PER_SAMPLE))
				throw new AppException(ErrorId.BITS_PER_SAMPLE_OUT_OF_BOUNDS);

			// Sample rate
			sampleRate = NumberUtils.bytesToIntBE(data, offset, SAMPLE_RATE_SIZE);
			offset += SAMPLE_RATE_SIZE;
			if ((sampleRate < MIN_SAMPLE_RATE) || (sampleRate > MAX_SAMPLE_RATE))
				throw new AppException(ErrorId.SAMPLE_RATE_OUT_OF_BOUNDS);

			// Number of sample frames
			numSampleFrames = NumberUtils.bytesToLongBE(data, offset, NUM_SAMPLE_FRAMES_SIZE);
			offset += NUM_SAMPLE_FRAMES_SIZE;
			if ((numSampleFrames < MIN_NUM_SAMPLE_FRAMES) || (numSampleFrames > MAX_NUM_SAMPLE_FRAMES))
				throw new AppException(ErrorId.NUM_SAMPLE_FRAMES_OUT_OF_BOUNDS);

			// CRC value
			crcValue = NumberUtils.bytesToLongBE(data, offset, CRC_SIZE) & 0xFFFFFFFFL;
			offset += CRC_SIZE;

			// Key length
			keyLength = NumberUtils.bytesToIntBE(data, offset, KEY_LENGTH_SIZE);
			offset += KEY_LENGTH_SIZE;
			if ((keyLength < MIN_KEY_LENGTH) || (keyLength > MAX_KEY_LENGTH))
				throw new AppException(ErrorId.KEY_LENGTH_OUT_OF_BOUNDS);

			// Block length
			blockLength = NumberUtils.bytesToIntBE(data, offset, BLOCK_LENGTH_SIZE);
			offset += BLOCK_LENGTH_SIZE;
			if ((blockLength < MIN_BLOCK_LENGTH) || (blockLength > MAX_BLOCK_LENGTH))
				throw new AppException(ErrorId.BLOCK_LENGTH_OUT_OF_BOUNDS);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public int getBytesPerSample()
		{
			return (bitsPerSample + 7 >> 3);
		}

		//--------------------------------------------------------------

		public int getBytesPerSampleFrame()
		{
			return (getBytesPerSample() * numChannels);
		}

		//--------------------------------------------------------------

		public byte[] getBytes()
		{
			byte[] buffer = new byte[SIZE];

			int offset = 0;
			NumberUtils.intToBytesBE(version, buffer, offset, VERSION_SIZE);
			offset += VERSION_SIZE;
			NumberUtils.intToBytesBE(numChannels, buffer, offset, NUM_CHANNELS_SIZE);
			offset += NUM_CHANNELS_SIZE;
			NumberUtils.intToBytesBE(bitsPerSample, buffer, offset, BITS_PER_SAMPLE_SIZE);
			offset += BITS_PER_SAMPLE_SIZE;
			NumberUtils.intToBytesBE(sampleRate, buffer, offset, SAMPLE_RATE_SIZE);
			offset += SAMPLE_RATE_SIZE;
			NumberUtils.longToBytesBE(numSampleFrames, buffer, offset, NUM_SAMPLE_FRAMES_SIZE);
			offset += NUM_SAMPLE_FRAMES_SIZE;
			NumberUtils.longToBytesBE(crcValue, buffer, offset, CRC_SIZE);
			offset += CRC_SIZE;
			NumberUtils.intToBytesBE(keyLength, buffer, offset, KEY_LENGTH_SIZE);
			offset += KEY_LENGTH_SIZE;
			NumberUtils.intToBytesBE(blockLength, buffer, offset, BLOCK_LENGTH_SIZE);
			offset += BLOCK_LENGTH_SIZE;

			return buffer;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		int		version;
		int		numChannels;
		int		bitsPerSample;
		int		sampleRate;
		long	numSampleFrames;
		long	crcValue;
		int		keyLength;
		int		blockLength;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// ATTRIBUTES WRITER CLASS


	private class AttributesWriter
		implements Chunk.IWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private AttributesWriter()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Chunk.IWriter interface
	////////////////////////////////////////////////////////////////////

		public boolean reset(int pass)
		{
			return true;
		}

		//--------------------------------------------------------------

		public long getLength()
		{
			return Attributes.SIZE;
		}

		//--------------------------------------------------------------

		public void write(DataOutput dataOutput)
			throws IOException
		{
			dataOutput.write(attributes.getBytes());
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// PRIVATE DATA WRITER CLASS


	private class PrivateDataWriter
		implements Chunk.IWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private PrivateDataWriter()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Chunk.IWriter interface
	////////////////////////////////////////////////////////////////////

		public boolean reset(int pass)
		{
			return false;
		}

		//--------------------------------------------------------------

		public long getLength()
		{
			return privateData.length;
		}

		//--------------------------------------------------------------

		public void write(DataOutput dataOutput)
			throws IOException
		{
			dataOutput.write(privateData);
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// DATA WRITER CLASS


	private class DataWriter
		implements Chunk.IWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private DataWriter(CompressedDataSource dataSource)
		{
			this.dataSource = dataSource;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Chunk.IWriter interface
	////////////////////////////////////////////////////////////////////

		public boolean reset(int pass)
		{
			return false;
		}

		//--------------------------------------------------------------

		public long getLength()
		{
			return -1;
		}

		//--------------------------------------------------------------

		public void write(DataOutput dataOutput)
			throws IOException
		{
			// Open compressed data output
			OndaDataOutput compressedDataOutput = new OndaDataOutput(attributes.numChannels,
																	 attributes.bitsPerSample,
																	 attributes.keyLength, dataOutput);

			// Write compressed sample data
			int[] buffer = new int[attributes.blockLength * attributes.numChannels];
			int bytesPerSample = attributes.getBytesPerSample();
			while (true)
			{
				IByteDataSource.ByteData data = null;
				try
				{
					data = dataSource.getData();
				}
				catch (AppException e)
				{
					throw new IOException(e);
				}
				if (data == null)
					break;

				int index = 0;
				int offset = data.offset;
				int endOffset = offset + data.length;
				while (offset < endOffset)
				{
					buffer[index++] = NumberUtils.bytesToIntLE(data.data, offset, bytesPerSample);
					offset += bytesPerSample;
				}
				compressedDataOutput.writeBlock(buffer, 0, index);
			}

			// Close compressed data output
			compressedDataOutput.close();

			// Set size of data
			dataSize = compressedDataOutput.getOutLength();

			// Set CRC value in attributes
			attributes.crcValue = dataSource.getCrc();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	CompressedDataSource	dataSource;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public OndaFile(File file)
	{
		this.file = file;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	private static void throwAppException(NlfException exception)
		throws AppException
	{
		// Throw any underlying AppException
		Throwable cause = exception.getCause();
		while (cause != null)
		{
			if (cause instanceof AppException)
				throw (AppException)cause;
			cause = cause.getCause();
		}

		// Get file offset
		StringBuilder buffer = new StringBuilder();
		long offset = exception.getOffset();
		if (offset >= 0)
		{
			buffer.append(OFFSET_STR);
			buffer.append(Long.toHexString(offset).toUpperCase());
		}

		// Get exception detail message
		String message = exception.getMessage();
		if (message != null)
		{
			if (buffer.length() > 0)
				buffer.append('\n');
			buffer.append(message);
		}

		// Construct and throw exception
		AppException outException = new AppException(buffer.toString(), exception.getCause());
		throw (exception.getFile() == null) ? outException
											: new FileException(outException, exception.getFile());
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

	public Attributes readAttributes()
		throws AppException
	{
		read(READ_ATTRIBUTES, null);
		return attributes;
	}

	//------------------------------------------------------------------

	public PrivateData readPrivateData()
		throws AppException
	{
		read(READ_PRIVATE_DATA, null);
		return ((privateData == null) ? null : new PrivateData(privateData));
	}

	//------------------------------------------------------------------

	public Attributes readAttributesAndPrivateData()
		throws AppException
	{
		read(READ_ATTRIBUTES | READ_PRIVATE_DATA, null);
		return attributes;
	}

	//------------------------------------------------------------------

	public Attributes readData(IByteDataOutputStream outStream)
		throws AppException
	{
		read(READ_ATTRIBUTES | READ_DATA, outStream);
		return attributes;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public long getDataSize()
	{
		return dataSize;
	}

	//------------------------------------------------------------------

	public void write(Attributes           attributes,
					  byte[]               privateData,
					  CompressedDataSource dataSource)
		throws AppException
	{
		// Set instance variables
		this.attributes = attributes;
		this.privateData = privateData;

		// Write file
		try
		{
			// Create NLF document and root list
			Document document = new Document(false);
			ChunkList rootList = document.createRootList(ONDA_ID);
			rootList.setNamespaceName(NAMESPACE_NAME);

			// Create attributes chunk
			Chunk attributesChunk = document.createChunk(ATTRIBUTES_ID);
			attributesChunk.setWriter(new AttributesWriter());
			rootList.appendChunk(attributesChunk);

			// Create private data chunk
			if (privateData != null)
			{
				Chunk privateDataChunk = document.createChunk(PRIVATE_DATA_ID);
				privateDataChunk.setWriter(new PrivateDataWriter());
				rootList.appendChunk(privateDataChunk);
			}

			// Create data chunk
			Chunk dataChunk = document.createChunk(DATA_ID);
			dataChunk.setWriter(new DataWriter(dataSource));
			rootList.appendChunk(dataChunk);

			// Set sizes of chunks from chunk writers
			rootList.updateSize();

			// Write document
			document.write(file);
		}
		catch (NlfException e)
		{
			throwAppException(e);
		}
	}

	//------------------------------------------------------------------

	private void read(int                   readKind,
					  IByteDataOutputStream outStream)
		throws AppException
	{
		// Initialise variables
		if ((readKind & READ_ATTRIBUTES) != 0)
			attributes = null;
		if ((readKind & READ_PRIVATE_DATA) != 0)
			privateData = null;
		List<Id> ids = new ArrayList<>();

		// Read document and parse its structure
		Document document = null;
		try
		{
			document = new Document(false);
			document.read(file);
		}
		catch (NlfException e)
		{
			throwAppException(e);
		}

		// Read chunk data
		try
		{
			// Validate namespace name of root list
			String namespaceName = document.getRootList().getNamespaceName();
			if (!namespaceName.matches(NAMESPACE_NAME_REGEX))
				throw new FileException(ErrorId.NOT_AN_ONDA_FILE, file);

			// Iterate over chunks in root list
			try
			{
				for (int i = 0; i < document.getRootList().getNumChunks(); i++)
				{
					// Get chunk ID
					Chunk chunk = document.getRootList().getChunk(i);
					Id id = chunk.getId();

					// Reset chunk reader
					chunk.getReader().reset();

					// Read attributes chunk
					if (id.equals(ATTRIBUTES_ID))
					{
						if (ids.contains(ATTRIBUTES_ID))
							throw new FileException(ErrorId.MULTIPLE_ATTRIBUTES_CHUNKS, file);

						if ((readKind & READ_ATTRIBUTES) != 0)
							readAttributes(chunk);
					}

					// Read private data chunk
					if (id.equals(PRIVATE_DATA_ID))
					{
						if (ids.contains(DATA_ID))
							throw new FileException(ErrorId.PRIVATE_CHUNK_AFTER_DATA_CHUNK, file);

						if (ids.contains(PRIVATE_DATA_ID))
							throw new FileException(ErrorId.MULTIPLE_PRIVATE_CHUNKS, file);

						if ((readKind & READ_PRIVATE_DATA) != 0)
							readPrivateData(chunk);
					}

					// Read data chunk
					if (id.equals(DATA_ID))
					{
						if (!ids.contains(ATTRIBUTES_ID))
							throw new FileException(ErrorId.NO_ATTRIBUTES_CHUNK_BEFORE_DATA_CHUNK, file);

						if (ids.contains(DATA_ID))
							throw new FileException(ErrorId.MULTIPLE_DATA_CHUNKS, file);

						if ((readKind & READ_DATA) != 0)
							readData(chunk, outStream);
					}

					// Add chunk ID to list
					ids.add(id);
				}
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.ERROR_READING_FILE, file, e);
			}

			// Close file
			try
			{
				document.close();
			}
			catch (IOException e)
			{
				throw new FileException(ErrorId.FAILED_TO_CLOSE_FILE, file, e);
			}
		}
		catch (AppException e)
		{
			if (document != null)
				document.closeIgnoreException();
			throw e;
		}

		// Test for missing chunks
		if (!ids.contains(ATTRIBUTES_ID))
			throw new FileException(ErrorId.NO_ATTRIBUTES_CHUNK, file);
		if (!ids.contains(DATA_ID))
			throw new FileException(ErrorId.NO_DATA_CHUNK, file);
	}

	//------------------------------------------------------------------

	private void readAttributes(Chunk chunk)
		throws AppException, IOException
	{
		// Test version
		if (chunk.getSize() < Attributes.VERSION_SIZE)
			throw new FileException(ErrorId.INVALID_ATTRIBUTES_CHUNK, file);
		byte[] buffer = new byte[Attributes.SIZE];
		chunk.getReader().getDataInput().readFully(buffer, 0, Attributes.VERSION_SIZE);
		int version = NumberUtils.bytesToIntBE(buffer, 0, Attributes.VERSION_SIZE);
		if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
			throw new FileException(ErrorId.UNSUPPORTED_VERSION, file, Integer.toString(version));

		// Parse attributes
		if (chunk.getSize() != Attributes.SIZE)
			throw new FileException(ErrorId.INVALID_ATTRIBUTES_CHUNK, file);
		chunk.getReader().getDataInput().readFully(buffer, Attributes.VERSION_SIZE,
												   Attributes.SIZE - Attributes.VERSION_SIZE);
		try
		{
			attributes = new Attributes(buffer, 0);
		}
		catch (AppException e)
		{
			throw new FileException(e, file);
		}
	}

	//------------------------------------------------------------------

	private void readPrivateData(Chunk chunk)
		throws AppException, IOException
	{
		// Test size of private data
		if (chunk.getSize() > Integer.MAX_VALUE)
			throw new FileException(ErrorId.PRIVATE_DATA_ARE_TOO_LARGE, file);

		// Read private data
		try
		{
			byte[] buffer = new byte[(int)chunk.getSize()];
			chunk.getReader().getDataInput().readFully(buffer);
			privateData = buffer;
		}
		catch (OutOfMemoryError e)
		{
			throw new FileException(ErrorId.NOT_ENOUGH_MEMORY, file);
		}
	}

	//------------------------------------------------------------------

	private void readData(Chunk                 chunk,
						  IByteDataOutputStream outStream)
		throws AppException
	{
		// Open compressed data input
		OndaDataInput compressedDataInput =
										new OndaDataInput(chunk.getSize(), attributes.numChannels,
														  attributes.bitsPerSample, attributes.keyLength,
														  chunk.getReader().getDataInput());

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
				throw new FileException(ErrorId.MALFORMED_FILE, file, e);
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	File		file;
	private	long		dataSize;
	private	Attributes	attributes;
	private	byte[]		privateData;

}

//----------------------------------------------------------------------
