/*====================================================================*\

OndaDataOutput.java

Onda lossless audio compression data output class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;

//----------------------------------------------------------------------


// ONDA LOSSLESS AUDIO COMPRESSION DATA OUTPUT CLASS


/**
 * This class implements a data output for compressing blocks of data with the Onda lossless audio
 * compression algorithm.  Blocks of compressed data are written to an underlying data destination in the
 * form specified by the <a href="http://onda.sourceforge.net/ondaAlgorithmAndFileFormats.html">Onda
 * algorithm</a>.
 * <p>
 * The underlying data destination for this output is an instance of {@code java.io.DataOutput}.  Data that
 * are written to the data destination are buffered to improve efficiency.
 * </p>
 * <p>
 * The implementation of this class in the Onda application works only for integer sample values of up to 24
 * bits per sample.  Above 24 bits per sample, the type of the instance variable {@code bitBuffer} must be
 * changed from {@code int} to {@code long} to accommodate the extra bits.
 * </p>
 *
 * @see OndaDataInput
 */

public class OndaDataOutput
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	BUFFER_SIZE	= 1 << 13;  // 8192

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Constructs an {@code OndaDataOutput} that has an instance of {@code java.io.DataOutput} as its
	 * underlying data destination.
	 *
	 * @param numChannels   the number of audio channels in the sample data.
	 * @param sampleLength  the length (in bits) of a sample value.
	 * @param keyLength     the length (in bits) of an encoding key.
	 * @param dataOutput    the underlying destination to which compressed data is to be written.
	 */

	public OndaDataOutput(int        numChannels,
						  int        sampleLength,
						  int        keyLength,
						  DataOutput dataOutput)
	{
		this.numChannels = numChannels;
		this.sampleLength = sampleLength;
		this.keyLength = keyLength;
		this.dataOutput = dataOutput;
		minEncodingLength = Math.max(1, sampleLength - (1 << keyLength) + 1);
		outBuffer = new byte[BUFFER_SIZE];
		encodingBounds = new int[sampleLength];
		for (int i = minEncodingLength; i < sampleLength; i++)
			encodingBounds[i] = (1 << i - 1) - 1;
		negEncodingBounds = new int[numChannels];
		posEncodingBounds = new int[numChannels];
		encodingLengths = new int[numChannels];
		excessCodes = new int[numChannels];
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the length of data that has been written to the underlying data destination.
	 *
	 * @return the length of data that has been written to the underlying data destination since the data
	 *         output was opened.
	 */

	public long getOutLength()
	{
		return outLength;
	}

	//------------------------------------------------------------------

	/**
	 * Closes the data output.  Any unwritten compressed data is written to the data destination.  This
	 * method does not close the underlying data destination.
	 *
	 * @throws IOException
	 *           if an error occurs while attempting to write to the data destination.
	 */

	public void close()
		throws IOException
	{
		// Write residual contents of buffer to data destination
		if (bitDataLength > 0)
			outBuffer[outBufferIndex++] = (byte)(bitBuffer << 8 - bitDataLength);

		if (outBufferIndex > 0)
		{
			dataOutput.write(outBuffer, 0, outBufferIndex);
			outLength += outBufferIndex;
			outBufferIndex = 0;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Compresses a block of sample data and writes the compressed data to the data destination.  The output
	 * data is written in the form of a data block of an Onda file (ie, a compression key for each channel,
	 * followed by interleaved encoded data).
	 * <p>
	 * The samples for multiple audio channels must be interleaved in the input data.
	 * </p>
	 *
	 * @param  data    the data that is to be compressed and written.
	 * @param  offset  the start offset of the sample data in {@code data}.
	 * @param  length  the number of samples that are to be written.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code data} is {@code null}, or</li>
	 *             <li>{@code (length < 0)} or {@code (length > data.length - offset)}.</li>
	 *           </ul>
	 * @throws IndexOutOfBoundsException
	 *           if {@code (offset < 0)} or {@code (offset > data.length)}.
	 * @throws IOException
	 *           if an error occurs while attempting to write to the data destination.
	 */

	public void writeBlock(int[] data,
						   int   offset,
						   int   length)
		throws IOException
	{
		// Validate arguments
		if (data == null)
			throw new IllegalArgumentException();
		if ((offset < 0) || (offset > data.length))
			throw new IndexOutOfBoundsException();
		if ((length < 0) || (length > data.length - offset))
			throw new IllegalArgumentException();

		// Get excess count for each encoding length
		int[][] excessCounts = new int[numChannels][sampleLength];
		int[] prevSampleValues = new int[numChannels];
		int[] prevDeltas = new int[numChannels];
		int startOffset = offset;
		int endOffset = startOffset + length;
		while (offset < endOffset)
		{
			for (int i = 0; i < numChannels; i++)
			{
				int sampleValue = data[offset];
				int delta = sampleValue - prevSampleValues[i];
				if (offset > startOffset)
				{
					int absEpsilon = Math.abs(delta - prevDeltas[i]);
					for (int j = sampleLength - 1; j >= minEncodingLength; j--)
					{
						if (absEpsilon > encodingBounds[j])
						{
							for (; j >= minEncodingLength; --j)
								++excessCounts[i][j];
						}
					}
				}
				prevSampleValues[i] = sampleValue;
				prevDeltas[i] = delta;

				++offset;
			}
		}

		// Determine optimum encoding length
		int numSampleFrames = length / numChannels;
		for (int i = 0; i < numChannels; i++)
		{
			encodingLengths[i] = sampleLength;
			long minOutputLength = numSampleFrames * sampleLength;
			for (int j = minEncodingLength; j < sampleLength; j++)
			{
				long outputLength = (numSampleFrames - 1) * j + (excessCounts[i][j] + 1) * sampleLength;
				if (minOutputLength > outputLength)
				{
					minOutputLength = outputLength;
					encodingLengths[i] = j;
				}
			}
		}

		// Write key for each channel; initialise per-channel encoding variables
		for (int i = 0; i < numChannels; i++)
		{
			write(sampleLength - encodingLengths[i], keyLength);

			prevSampleValues[i] = 0;
			prevDeltas[i] = 0;
			int value = 1 << encodingLengths[i] - 1;
			excessCodes[i] = value;
			--value;
			posEncodingBounds[i] = value;
			negEncodingBounds[i] = -value;
		}

		// Encode sample data and write them to data destination
		offset = startOffset;
		while (offset < endOffset)
		{
			for (int i = 0; i < numChannels; i++)
			{
				int sampleValue = data[offset];
				int delta = sampleValue - prevSampleValues[i];

				if ((offset == startOffset) || (encodingLengths[i] == sampleLength))
					write(sampleValue, sampleLength);
				else
				{
					int epsilon = delta - prevDeltas[i];
					if ((epsilon < negEncodingBounds[i]) || (epsilon > posEncodingBounds[i]))
					{
						write(excessCodes[i], encodingLengths[i]);
						write(sampleValue, sampleLength);
					}
					else
						write(epsilon, encodingLengths[i]);
				}
				prevSampleValues[i] = sampleValue;
				prevDeltas[i] = delta;

				++offset;
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * Writes a bit string of a specified length to the data destination.
	 *
	 * @param  value   the bit string that is to be written.
	 * @param  length  the number of low-order bits of {@code value} to write.
	 * @throws IOException
	 *           if an error occurs while attempting to write to the data destination.
	 */

	private void write(int value,
					   int length)
		throws IOException
	{
		bitBuffer <<= length;
		bitBuffer |= value & ((1 << length) - 1);
		bitDataLength += length;
		while (bitDataLength >= 8)
		{
			bitDataLength -= 8;
			outBuffer[outBufferIndex++] = (byte)(bitBuffer >>> bitDataLength);
			if (outBufferIndex >= outBuffer.length)
			{
				dataOutput.write(outBuffer);
				outLength += outBuffer.length;
				outBufferIndex = 0;
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	DataOutput	dataOutput;
	private	int			numChannels;
	private	int			sampleLength;
	private	int			minEncodingLength;
	private	int			keyLength;
	private	int			bitBuffer;
	private	int			bitDataLength;
	private	int			outBufferIndex;
	private	byte[]		outBuffer;
	private	int[]		encodingBounds;
	private	int[]		negEncodingBounds;
	private	int[]		posEncodingBounds;
	private	int[]		encodingLengths;
	private	int[]		excessCodes;
	private	long		outLength;

}

//----------------------------------------------------------------------
