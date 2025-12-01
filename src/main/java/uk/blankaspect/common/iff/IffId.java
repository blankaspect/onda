/*====================================================================*\

IffId.java

IFF identifier class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// IFF IDENTIFIER CLASS


public class IffId
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final		int	SIZE	= 4;

	public static final		int	MIN_CHAR	= '\u0020';
	public static final		int	MAX_CHAR	= '\u007E';

	private static final	byte[]	DEFAULT_VALUE	= { ' ', ' ', ' ', ' ' };

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	int	value;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public IffId()
	{
		value = NumberCodec.bytesToUIntBE(DEFAULT_VALUE);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public IffId(byte[] bytes)
	{
		set(bytes);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public IffId(byte[] bytes,
				 int    offset)
	{
		set(bytes, offset);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public IffId(String str)
	{
		set(str);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof IffId other) && (value == other.value);
	}

	//------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		return value;
	}

	//------------------------------------------------------------------

	@Override
	public String toString()
	{
		return new String(getBytes(), StandardCharsets.US_ASCII);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int getValue()
	{
		return value;
	}

	//------------------------------------------------------------------

	public byte[] getBytes()
	{
		byte[] buffer = new byte[SIZE];
		NumberCodec.uIntToBytesBE(value, buffer);
		return buffer;
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public void set(byte[] bytes)
	{
		set(bytes, 0);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public void set(byte[] bytes,
					int    offset)
	{
		if ((offset < 0) || (offset > bytes.length - SIZE))
			throw new IndexOutOfBoundsException();

		for (int i = 0; i < SIZE; i++)
		{
			int ch = bytes[i + offset] & 0xFF;
			if ((ch < MIN_CHAR) || (ch > MAX_CHAR) || ((i == 0) && (ch == ' ')))
				throw new IllegalArgumentException();
		}
		value = NumberCodec.bytesToUIntBE(bytes, offset, SIZE);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 */

	public void set(String str)
	{
		set(str.getBytes(StandardCharsets.US_ASCII), 0);
	}

	//------------------------------------------------------------------

	public void put(byte[] buffer)
	{
		NumberCodec.uIntToBytesBE(value, buffer, 0, SIZE);
	}

	//------------------------------------------------------------------

	public void put(byte[] buffer,
					int    offset)
	{
		NumberCodec.uIntToBytesBE(value, buffer, offset, SIZE);
	}

	//------------------------------------------------------------------

	public void write(OutputStream outStream)
		throws IOException
	{
		outStream.write(getBytes());
	}

	//------------------------------------------------------------------

	public void write(DataOutput dataOutput)
		throws IOException
	{
		dataOutput.write(getBytes());
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
