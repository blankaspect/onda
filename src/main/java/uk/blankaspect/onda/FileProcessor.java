/*====================================================================*\

FileProcessor.java

File processor class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.CRC32;

import uk.blankaspect.common.audio.AudioFile;

import uk.blankaspect.common.bytedata.IByteDataOutputStream;
import uk.blankaspect.common.bytedata.IByteDataSource;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.FileException;
import uk.blankaspect.common.exception.TaskCancelledException;

import uk.blankaspect.common.iff.Chunk;
import uk.blankaspect.common.iff.ChunkFilter;
import uk.blankaspect.common.iff.IffId;

//----------------------------------------------------------------------


// FILE PROCESSOR CLASS


class FileProcessor
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	READING_STR				= "Reading";
	private static final	String	WRITING_STR				= "Writing";
	private static final	String	COMPRESSED_STR			= "The file was compressed.";
	private static final	String	EXPANDED_STR			= "The file was expanded.";
	private static final	String	VALID_STR				= "The file was valid.";
	private static final	String	SECONDS_STR				= " seconds";
	private static final	String	PRESERVED_CHUNKS_STR	= "Preserved chunks: ";

	private static final	DecimalFormat	FORMAT	= new DecimalFormat("0.0");

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

		UNRECOGNISED_FILE_KIND
		("The input file is not a recognised kind of audio file."),

		INCONSISTENT_FILE_KINDS
		("The input file is a different kind of audio file from the output file."),

		INCORRECT_CRC
		("The checksum of the sample data is incorrect."),

		UNSUPPORTED_NUM_CHANNELS
		("The file has %1 channels.\n" + App.SHORT_NAME + " works only with files that have between " +
			OndaFile.MIN_NUM_CHANNELS + " and " + OndaFile.MAX_NUM_CHANNELS + " channels."),

		UNSUPPORTED_SAMPLE_RATE
		("The file has a sample rate of %1 Hz.\n" + App.SHORT_NAME + " works only with files that " +
			"have a sample rate between " + OndaFile.MIN_SAMPLE_RATE + " Hz and " +
			OndaFile.MAX_SAMPLE_RATE + " Hz."),

		UNSUPPORTED_BITS_PER_SAMPLE
		("The file has %1 bits per sample.\n" + App.SHORT_NAME + " works only with files that have " +
			"16 or 24 bits per sample."),

		TOO_MANY_SAMPLE_FRAMES
		("The file contains too many sample frames for " + App.SHORT_NAME + ".");

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
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// VALIDATION RESULT CLASS


	public static class ValidationResult
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public ValidationResult()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		int	numFound;
		int	numValidated;
		int	numValid;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// COMPRESSOR CLASS


	private class Compressor
		implements IByteDataOutputStream, OndaFile.CompressedDataSource
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Compressor()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ByteDataOutputStream interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs in Task.Compress (primary thread).
		 */

		public synchronized void write(byte[] buffer,
									   int    offset,
									   int    length)
			throws AppException
		{
			try
			{
				while (length > 0)
				{
					// Wait for sample data buffer to become free
					while (!Task.isExceptionOrCancelled() && (sampleData != null))
					{
						try
						{
							wait(100);
						}
						catch (InterruptedException e)
						{
							// ignore
						}
					}

					// Test whether an exception has occurred in another thread or the task has been
					// cancelled
					Task.throwIfExceptionOrCancelled();

					// Allocate buffer for block
					if (blockBuffer == null)
					{
						blockBuffer = new byte[Math.min(numSampleFrames - inSampleFrameIndex,
														AppConfig.INSTANCE.getBlockLength()) *
																					bytesPerSampleFrame];
						blockBufferOffset = 0;
					}

					// Copy sample data to block buffer
					int copyLength = Math.min(length, blockBuffer.length - blockBufferOffset);
					System.arraycopy(buffer, offset, blockBuffer, blockBufferOffset, copyLength);

					// Update offsets and length
					blockBufferOffset += copyLength;
					offset += copyLength;
					length -= copyLength;

					// Make sample data available for output
					if (blockBufferOffset == blockBuffer.length)
					{
						// Update CRC
						crc.update(blockBuffer);

						// Increment sample frame index
						inSampleFrameIndex += blockBuffer.length / bytesPerSampleFrame;

						// Make sample data available to output thread
						sampleData = blockBuffer;

						// Free block buffer
						blockBuffer = null;
					}

					// Wake up waiting threads
					notifyAll();
				}
			}
			catch (AppException e)
			{
				notifyAll();
				throw e;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : CompressedDataSource interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs in Task.WriteCompressed (secondary thread).
		 */

		public synchronized IByteDataSource.ByteData getData()
			throws AppException
		{
			// Get sample data
			IByteDataSource.ByteData data = null;
			if (outSampleFrameIndex < numSampleFrames)
			{
				// Wait for sample data
				while (!Task.isCancelled() && (sampleData == null))
				{
					try
					{
						wait(100);
					}
					catch (InterruptedException e)
					{
						// ignore
					}
				}

				// Test whether task has been cancelled
				if (Task.isCancelled())
				{
					notifyAll();
					throw new TaskCancelledException();
				}

				// Create sample data object
				data = new IByteDataSource.ByteData(sampleData);
				outSampleFrameIndex += sampleData.length / bytesPerSampleFrame;

				// Free sample data buffer
				sampleData = null;
			}

			// Wake up waiting threads
			notifyAll();

			// Update progress of task
			Task.setProgress((double)outSampleFrameIndex / (double)numSampleFrames);

			// Return sample data
			return data;
		}

		//--------------------------------------------------------------

		/**
		 * Runs in Task.WriteCompressed (secondary thread).
		 */

		public long getCrc()
		{
			return crc.getValue();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public void init()
		{
			blockBuffer = null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		blockBufferOffset;
		private	byte[]	blockBuffer;

	}

	//==================================================================


	// EXPANDER CLASS


	private class Expander
		implements IByteDataOutputStream, IByteDataSource
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Expander()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ByteDataOutputStream interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs in Task.Expand (primary thread).
		 */

		public synchronized void write(byte[] buffer,
									   int    offset,
									   int    length)
			throws AppException
		{
			try
			{
				// Wait for sample data buffer to become free
				while (!Task.isExceptionOrCancelled() && (sampleData != null))
				{
					try
					{
						wait(100);
					}
					catch (InterruptedException e)
					{
						// ignore
					}
				}

				// Test whether an exception has occurred in another thread or the task has been cancelled
				Task.throwIfExceptionOrCancelled();

				// Update CRC
				crc.update(buffer, offset, length);

				// Copy sample data to buffer
				sampleData = new byte[length];
				System.arraycopy(buffer, offset, sampleData, 0, length);

				// Wake up waiting threads
				notifyAll();
			}
			catch (AppException e)
			{
				notifyAll();
				throw e;
			}
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IByteDataSource interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs in Task.WriteExpanded (secondary thread).
		 */

		public void reset()
		{
			// do nothing
		}

		//--------------------------------------------------------------

		public long getLength()
		{
			return (numSampleFrames * bytesPerSampleFrame);
		}

		//--------------------------------------------------------------

		/**
		 * Runs in Task.WriteExpanded (secondary thread).
		 */

		public synchronized IByteDataSource.ByteData getData()
			throws AppException
		{
			// Get sample data
			IByteDataSource.ByteData data = null;
			if (outSampleFrameIndex < numSampleFrames)
			{
				// Wait for sample data
				while (!Task.isCancelled() && (sampleData == null))
				{
					try
					{
						wait(100);
					}
					catch (InterruptedException e)
					{
						// ignore
					}
				}

				// Test whether task has been cancelled
				if (Task.isCancelled())
				{
					notifyAll();
					throw new TaskCancelledException();
				}

				// Create sample data object
				data = new IByteDataSource.ByteData(sampleData);
				outSampleFrameIndex += sampleData.length / bytesPerSampleFrame;

				// Free sample data buffer
				sampleData = null;
			}

			// Wake up waiting threads
			notifyAll();

			// Update progress of task
			Task.setProgress((double)outSampleFrameIndex / (double)numSampleFrames);

			// Return sample data
			return data;
		}

		//--------------------------------------------------------------

	}

	//==================================================================


	// VALIDATOR CLASS


	private class Validator
		implements IByteDataOutputStream
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Validator()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : ByteDataOutputStream interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Runs in Task.Validate (primary thread).
		 */

		public synchronized void write(byte[] buffer,
									   int    offset,
									   int    length)
			throws AppException
		{
			// Test whether task has been cancelled
			Task.throwIfCancelled();

			// Update CRC
			crc.update(buffer, offset, length);

			// Increment sample frame index
			inSampleFrameIndex += length / bytesPerSampleFrame;

			// Update progress of task
			Task.setProgress((double)inSampleFrameIndex / (double)numSampleFrames);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FileProcessor()
	{
		compressor = new Compressor();
		expander = new Expander();
		validator = new Validator();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Runs in Task.Compress (primary thread).
	 */

	public void compress(File          inFile,
						 File          outFile,
						 ChunkFilter[] chunkFilters)
		throws AppException
	{
		// Get system time
		long startTime = System.currentTimeMillis();

		// Update information field in progress view
		Task.setInfo(READING_STR, inFile);
		Task.setProgress(0.0);

		// Determine kind of input file
		AudioFileKind audioFileKind = AudioFileKind.forFile(inFile);
		if (audioFileKind == null)
			throw new FileException(ErrorId.UNRECOGNISED_FILE_KIND, inFile);

		// Read input file attributes
		AudioFile audioFile = audioFileKind.createFile(inFile);
		audioFile.readAttributes();

		// Validate number of channels, bits per sample and sample rate
		numChannels = audioFile.getNumChannels();
		if ((numChannels < OndaFile.MIN_NUM_CHANNELS) || (numChannels > OndaFile.MAX_NUM_CHANNELS))
			throw new FileException(ErrorId.UNSUPPORTED_NUM_CHANNELS, inFile,
									Integer.toString(numChannels));

		bitsPerSample = BitsPerSample.forNumBits(audioFile.getBitsPerSample());
		if (bitsPerSample == null)
			throw new FileException(ErrorId.UNSUPPORTED_BITS_PER_SAMPLE, inFile,
									Integer.toString(audioFile.getBitsPerSample()));
		bytesPerSampleFrame = bitsPerSample.getBytesPerSample() * numChannels;

		sampleRate = audioFile.getSampleRate();
		if ((sampleRate < OndaFile.MIN_SAMPLE_RATE) || (sampleRate > OndaFile.MAX_SAMPLE_RATE))
			throw new FileException(ErrorId.UNSUPPORTED_SAMPLE_RATE, inFile,
									Integer.toString(sampleRate));

		// Set number of sample frames
		numSampleFrames = audioFile.getNumSampleFrames();

		// Compress private chunks in input file
		byte[] compressedPrivateData = null;
		List<IffId> ids = null;
		ChunkFilter chunkFilter = chunkFilters[audioFileKind.ordinal()];
		if (!chunkFilter.isExcludeAll())
		{
			PrivateData privateData = new PrivateData(audioFileKind);
			audioFile.read(privateData.getReader(chunkFilter));
			ids = privateData.getAncillaryIds();
			if (!ids.isEmpty())
				compressedPrivateData = privateData.getCompressedData();
		}

		// Initialise variables
		inSampleFrameIndex = 0;
		outSampleFrameIndex = 0;
		sampleData = null;
		crc = new CRC32();
		compressor.init();

		// Update information field in progress view
		Task.setInfo(WRITING_STR, outFile);

		// Start thread that writes output file
		new Task.WriteCompressed(this, outFile, compressedPrivateData).start();

		// Read sample data from input file
		audioFile.readInteger(compressor, null);

		// Wait for writing thread to finish
		while (Task.getNumThreads() > 1)
		{
			Thread.yield();
		}

		// Append result to log
		if ((ids != null) && !ids.isEmpty())
			Log.INSTANCE.appendLine(PRESERVED_CHUNKS_STR + Utils.listToString(ids));

		double compressionFactor = (double)compressedDataSize /
													(double)(numSampleFrames * bytesPerSampleFrame) * 100.0;
		double seconds = (double)(System.currentTimeMillis() - startTime) * 0.001;
		Log.INSTANCE.appendLine(COMPRESSED_STR + "  [ " + FORMAT.format(compressionFactor) +
													"%, " + FORMAT.format(seconds) + SECONDS_STR + " ]");
	}

	//------------------------------------------------------------------

	/**
	 * Runs in Task.Expand (primary thread).
	 */

	public void expand(File          inFile,
					   File          outFile,
					   AudioFileKind audioFileKind)
		throws AppException
	{
		// Get system time
		long startTime = System.currentTimeMillis();

		// Update information field in progress view
		Task.setInfo(READING_STR, inFile);
		Task.setProgress(0.0);

		// Read attributes and private data; set attributes
		OndaFileReader ondaFile = OndaFileIff.getFileKind(inFile).createReader(inFile);
		setAttributes(inFile, ondaFile.readAttributesAndPrivateData());

		// Convert private data to chunks
		List<IffId> ids = null;
		List<Chunk> chunks = null;
		if (ondaFile.getPrivateData() != null)
		{
			try
			{
				PrivateData privateData = new PrivateData(ondaFile.getPrivateData());
				if (privateData.getSourceKind() != audioFileKind)
					throw new AppException(ErrorId.INCONSISTENT_FILE_KINDS);
				ids = privateData.getAncillaryIds();
				int numChunks = privateData.getNumChunks();
				chunks = new ArrayList<>();
				for (int i = 0; i < numChunks; i++)
				{
					Chunk chunk = audioFileKind.createChunk();
					privateData.setChunk(i, chunk);
					chunks.add(chunk);
				}
			}
			catch (AppException e)
			{
				throw new FileException(e, inFile);
			}
		}

		// Initialise variables
		outSampleFrameIndex = 0;
		sampleData = null;
		crc = new CRC32();

		// Update information field in progress view
		Task.setInfo(WRITING_STR, outFile);

		// Start thread that writes output file
		new Task.WriteExpanded(this, outFile, audioFileKind, chunks).start();

		// Read and expand compressed sample data
		ondaFile.readData(expander);

		// Validate CRC
		if (crc.getValue() != crcValue)
			throw new FileException(ErrorId.INCORRECT_CRC, inFile);

		// Wait for other thread to finish
		while (Task.getNumThreads() > 1)
		{
			Thread.yield();
		}

		// Set timestamp of output file to that of input file
		outFile.setLastModified(inFile.lastModified());

		// Append result to log
		if ((ids != null) && !ids.isEmpty())
			Log.INSTANCE.appendLine(PRESERVED_CHUNKS_STR + Utils.listToString(ids));

		double seconds = (double)(System.currentTimeMillis() - startTime) * 0.001;
		Log.INSTANCE.
						appendLine(EXPANDED_STR + "  [ " + FORMAT.format(seconds) + SECONDS_STR + " ]");
	}

	//------------------------------------------------------------------

	/**
	 * Runs in Task.Validate (primary thread).
	 */

	public void validate(File             file,
						 ValidationResult validationResult)
		throws AppException
	{
		// Get system time
		long startTime = System.currentTimeMillis();

		// Update information field in progress view
		Task.setInfo(READING_STR, file);
		Task.setProgress(0.0);

		// Increment count of files found
		++validationResult.numFound;

		// Read attributes and private data; set attributes
		OndaFileReader ondaFile = OndaFileIff.getFileKind(file).createReader(file);
		setAttributes(file, ondaFile.readAttributesAndPrivateData());

		// Validate any private data
		List<IffId> ids = null;
		if (ondaFile.getPrivateData() != null)
		{
			try
			{
				PrivateData privateData = new PrivateData(ondaFile.getPrivateData());
				ids = privateData.getAncillaryIds();
			}
			catch (AppException e)
			{
				throw new FileException(e, file);
			}
		}

		// Initialise variables
		inSampleFrameIndex = 0;
		crc = new CRC32();

		// Read file
		ondaFile.readData(validator);

		// Increment count of files validated
		++validationResult.numValidated;

		// Validate CRC
		if (crc.getValue() != crcValue)
			throw new FileException(ErrorId.INCORRECT_CRC, file);

		// Increment count of valid files
		++validationResult.numValid;

		// Append result to log
		if ((ids != null) && !ids.isEmpty())
			Log.INSTANCE.appendLine(PRESERVED_CHUNKS_STR + Utils.listToString(ids));

		double seconds = (double)(System.currentTimeMillis() - startTime) * 0.001;
		Log.INSTANCE.appendLine(VALID_STR + "  [ " + numChannels + ", " +
													bitsPerSample.getNumBits() + ", " + sampleRate + ", " +
													numSampleFrames + "; " +
													FORMAT.format(seconds) + SECONDS_STR + " ]");
	}

	//------------------------------------------------------------------

	/**
	 * Runs in Task.WriteCompressed (secondary thread).
	 */

	public void writeCompressedFile(File   file,
									byte[] privateData)
		throws AppException
	{
		// Write file
		OndaFile.Attributes attributes =
								new OndaFile.Attributes((privateData == null) ? 0 : 1, numChannels,
														bitsPerSample.getNumBits(), sampleRate,
														numSampleFrames, 0, bitsPerSample.getKeyLength(),
														AppConfig.INSTANCE.getBlockLength());
		OndaFile ondaFile = new OndaFile(file);
		ondaFile.write(attributes, privateData, compressor);

		// Set size of compressed data
		compressedDataSize = ondaFile.getDataSize();
	}

	//------------------------------------------------------------------

	/**
	 * Runs in Task.WriteExpanded (secondary thread).
	 */

	public void writeExpandedFile(File          file,
								  AudioFileKind fileKind,
								  List<Chunk>   chunks)
		throws AppException
	{
		AudioFile audioFile = fileKind.createFile(file, numChannels, bitsPerSample.getNumBits(),
												  sampleRate);
		if (chunks != null)
			audioFile.addChunks(chunks);
		audioFile.write(expander);
	}

	//------------------------------------------------------------------

	private void setAttributes(File                file,
							   OndaFile.Attributes attributes)
		throws AppException
	{
		numChannels = attributes.numChannels;
		bitsPerSample = BitsPerSample.forNumBits(attributes.bitsPerSample);
		if (bitsPerSample == null)
			throw new FileException(ErrorId.UNSUPPORTED_BITS_PER_SAMPLE, file,
									Integer.toString(attributes.bitsPerSample));
		bytesPerSampleFrame = bitsPerSample.getBytesPerSample() * numChannels;
		sampleRate = attributes.sampleRate;
		if (attributes.numSampleFrames > Integer.MAX_VALUE / bytesPerSampleFrame)
			throw new FileException(ErrorId.TOO_MANY_SAMPLE_FRAMES, file);
		numSampleFrames = (int)attributes.numSampleFrames;
		crcValue = attributes.crcValue & 0xFFFFFFFFL;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Compressor		compressor;
	private	Expander		expander;
	private	Validator		validator;
	private	int				numChannels;
	private	BitsPerSample	bitsPerSample;
	private	int				bytesPerSampleFrame;
	private	int				sampleRate;
	private	int				numSampleFrames;
	private	long			crcValue;
	private	int				inSampleFrameIndex;
	private	int				outSampleFrameIndex;
	private	byte[]			sampleData;
	private	CRC32			crc;
	private	long			compressedDataSize;

}

//----------------------------------------------------------------------
