/*====================================================================*\

OndaDataInput.java

Onda lossless audio compression data input class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

//----------------------------------------------------------------------


// CLASS: ONDA LOSSLESS AUDIO COMPRESSION DATA INPUT


/**
 * This class implements a data input for decompressing blocks of data that have been compressed with the Onda lossless
 * audio compression algorithm.  Input data is assumed to be in the form of a sequence of data  blocks, as specified by
 * the <a href="https://blankaspect.github.io/onda/algorithm/ondaAlgorithmAndFileFormats.html">Onda algorithm</a>.
 * <p>
 * The underlying data source for this input is an instance of {@code java.io.DataInput}.  Data that is read from the
 * data source is buffered to improve efficiency.
 * </p>
 * <p>
 * The implementation of this class in the Onda application works only for integer sample values of up to 24 bits per
 * sample.  Above 24 bits per sample, the type of the instance variable {@code bitBuffer} must be changed from {@code
 * int} to {@code long} to accommodate the extra bits.
 * </p>
 *
 * @see OndaDataOutput
 */

public class OndaDataInput
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	BUFFER_LENGTH	= 1 << 13;  // 8192

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	DataInput	dataInput;
	private	long		dataLength;
	private	int			numChannels;
	private	int			sampleLength;
	private	int			keyLength;
	private	int			bitBuffer;
	private	int			bitDataLength;
	private	int			inBufferIndex;
	private	byte[]		inBuffer;
	private	int[]		encodingLengths;
	private	int[]		excessCodes;
	private	int[]		epsilonMasks;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@code OndaDataInput} that has an instance of {@code java.io.DataInput} as its underlying data
	 * source.
	 *
	 * @param dataLength
	 *          the length (in bytes) of the input data.
	 * @param numChannels
	 *          the number of audio channels in the sample data.
	 * @param sampleLength
	 *          the length (in bits) of a sample value.
	 * @param keyLength
	 *          the length (in bits) of an encoding key.
	 * @param dataInput
	 *          the underlying source from which compressed data is to be read.
	 */

	public OndaDataInput(
		long		dataLength,
		int			numChannels,
		int			sampleLength,
		int			keyLength,
		DataInput	dataInput)
	{
		this.dataLength = dataLength;
		this.numChannels = numChannels;
		this.sampleLength = sampleLength;
		this.keyLength = keyLength;
		this.dataInput = dataInput;
		inBuffer = new byte[BUFFER_LENGTH];
		inBufferIndex = inBuffer.length;
		encodingLengths = new int[numChannels];
		excessCodes = new int[numChannels];
		epsilonMasks = new int[numChannels];
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Reads a block of compressed data from the data source, and decompresses the data into the specified buffer.  The
	 * input data must be in the form of a data block of an Onda file (ie, a compression key for each channel, followed
	 * by interleaved encoded data).
	 *
	 * @param  buffer
	 *           the buffer in which the decompressed data is to be stored.
	 * @param  offset
	 *           the start offset at which sample data is to be stored in {@code buffer}.
	 * @param  length
	 *           the number of samples that are to be read.
	 * @throws IllegalArgumentException
	 *           <ul>
	 *             <li>{@code buffer} is {@code null}, or</li>
	 *             <li>{@code (length < 0)} or {@code (length > buffer.length - offset)}.</li>
	 *           </ul>
	 * @throws IndexOutOfBoundsException
	 *           if {@code (offset < 0)} or {@code (offset > buffer.length)}.
	 * @throws IOException
	 *           if an error occurs when attempting to read from the data source.
	 */

	public void readBlock(
		int[]	buffer,
		int		offset,
		int		length)
		throws IOException
	{
		// Validate arguments
		if (buffer == null)
			throw new IllegalArgumentException();
		if ((offset < 0) || (offset > buffer.length))
			throw new IndexOutOfBoundsException();
		if ((length < 0) || (length > buffer.length - offset))
			throw new IllegalArgumentException();

		// Get encoding length for each channel from key; initialise per-channel encoding variables
		for (int i = 0; i < numChannels; i++)
		{
			encodingLengths[i] = sampleLength - read(keyLength);
			excessCodes[i] = 1 << encodingLengths[i] - 1;
			epsilonMasks[i] = ~(excessCodes[i] - 1);
		}

		// Read sample data from source, decode them and write them to buffer
		int[] prevSampleValues = new int[numChannels];
		int[] prevDeltas = new int[numChannels];
		int sampleValue = 0;
		int delta = 0;
		boolean sampleValueExpected = false;
		int startOffset = offset;
		int endOffset = startOffset + length;
		while (offset < endOffset)
		{
			for (int i = 0; i < numChannels; i++)
			{
				if ((offset == startOffset) || (encodingLengths[i] == sampleLength))
					sampleValue = read(sampleLength);
				else
				{
					while (true)
					{
						if (sampleValueExpected)
						{
							sampleValue = read(sampleLength);
							sampleValueExpected = false;
							break;
						}
						else
						{
							int epsilon = read(encodingLengths[i]);
							if (epsilon == excessCodes[i])
								sampleValueExpected = true;
							else
							{
								if ((epsilon & excessCodes[i]) != 0)
									epsilon |= epsilonMasks[i];
								delta = prevDeltas[i] + epsilon;
								sampleValue = prevSampleValues[i] + delta;
								break;
							}
						}
					}
				}
				prevDeltas[i] = sampleValue - prevSampleValues[i];
				prevSampleValues[i] = sampleValue;

				buffer[offset++] = sampleValue;
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * Reads a bit string of a specified length from the data source.
	 *
	 * @param  length
	 *           the number of bits to read.
	 * @return the bit string that was read from the data source, as an unsigned integer.
	 * @throws IOException
	 *           if an error occurs when attempting to read from the data source.
	 */

	private int read(
		int	length)
		throws IOException
	{
		while (bitDataLength < length)
		{
			if (inBufferIndex >= inBuffer.length)
			{
				if (dataLength == 0)
					throw new EOFException();
				int readLength = (int)Math.min(dataLength, inBuffer.length);
				inBufferIndex = inBuffer.length - readLength;
				dataInput.readFully(inBuffer, inBufferIndex, readLength);
				dataLength -= readLength;
			}
			bitBuffer <<= 8;
			bitBuffer |= inBuffer[inBufferIndex++] & 0xFF;
			bitDataLength += 8;
		}
		bitDataLength -= length;
		return (bitBuffer >>> bitDataLength & ((1 << length) - 1));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
