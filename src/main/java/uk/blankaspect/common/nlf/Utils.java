/*====================================================================*\

Utils.java

Class: internal utility methods.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// CLASS: INTERNAL UTILITY METHODS


/**
 * This class contains a number of utility methods that are used by classes in the Nested-List File package.
 *
 * @since 1.0
 */

class Utils
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Prevents this class from being instantiated externally.
	 */

	private Utils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Converts a sequence of bytes with the specified byte order to an {@code int}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @param  littleEndian
	 *           {@code true} if the byte order of the byte sequence is little-endian; {@code false} if the byte order
	 *           is big-endian.
	 * @return the {@code int} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #intToBytes(int, byte[], int, int, boolean)
	 * @see    #bytesToLong(byte[], int, int, boolean)
	 */

	protected static int bytesToInt(
		byte[]	bytes,
		int		offset,
		int		length,
		boolean	littleEndian)
	{
		return littleEndian ? bytesToIntLE(bytes, offset, length) : bytesToIntBE(bytes, offset, length);
	}

	//------------------------------------------------------------------

	/**
	 * Converts an {@code int} to a sequence of bytes of the specified length and with the specified byte order.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @param littleEndian
	 *          if {@code true}, the byte order of the byte sequence will be little-endian; otherwise, the byte order
	 *          will be big-endian.
	 * @since 1.0
	 * @see   #bytesToInt(byte[], int, int, boolean)
	 * @see   #longToBytes(long, byte[], int, int, boolean)
	 */

	protected static void intToBytes(
		int		value,
		byte[]	buffer,
		int		offset,
		int		length,
		boolean	littleEndian)
	{
		if (littleEndian)
			intToBytesLE(value, buffer, offset, length);
		else
			intToBytesBE(value, buffer, offset, length);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a sequence of bytes with the specified byte order to a {@code long}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @param  littleEndian
	 *           {@code true} if the byte order of the byte sequence is little-endian; {@code false} if the byte order
	 *           is big-endian.
	 * @return the {@code long} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #longToBytes(long, byte[], int, int, boolean)
	 * @see    #bytesToInt(byte[], int, int, boolean)
	 */

	protected static long bytesToLong(
		byte[]	bytes,
		int		offset,
		int		length,
		boolean	littleEndian)
	{
		return littleEndian ? bytesToLongLE(bytes, offset, length) : bytesToLongBE(bytes, offset, length);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a {@code long} to a sequence of bytes of the specified length and with the specified byte order.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @param littleEndian
	 *          if {@code true}, the byte order of the byte sequence will be little-endian; otherwise, the byte order
	 *          will be big-endian.
	 * @since 1.0
	 * @see   #bytesToLong(byte[], int, int, boolean)
	 * @see   #intToBytes(int, byte[], int, int, boolean)
	 */

	protected static void longToBytes(
		long	value,
		byte[]	buffer,
		int		offset,
		int		length,
		boolean	littleEndian)
	{
		if (littleEndian)
			longToBytesLE(value, buffer, offset, length);
		else
			longToBytesBE(value, buffer, offset, length);
	}

	//------------------------------------------------------------------

	/**
	 * Converts a big-endian sequence of bytes to an {@code int}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @return the {@code int} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #bytesToIntLE(byte[], int, int)
	 * @see    #intToBytesBE(int, byte[], int, int)
	 */

	private static int bytesToIntBE(
		byte[]	bytes,
		int		offset,
		int		length)
	{
		int endOffset = offset + length;
		int value = bytes[offset];
		while (++offset < endOffset)
		{
			value <<= Byte.SIZE;
			value |= bytes[offset] & 0xFF;
		}
		return value;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a little-endian sequence of bytes to an {@code int}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @return the {@code int} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #bytesToIntBE(byte[], int, int)
	 * @see    #intToBytesLE(int, byte[], int, int)
	 */

	private static int bytesToIntLE(
		byte[]	bytes,
		int		offset,
		int		length)
	{
		int i = offset + length;
		int value = bytes[--i];
		while (--i >= offset)
		{
			value <<= Byte.SIZE;
			value |= bytes[i] & 0xFF;
		}
		return value;
	}

	//------------------------------------------------------------------

	/**
	 * Converts an {@code int} to a big-endian sequence of bytes of the specified length.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @since 1.0
	 * @see   #intToBytesLE(int, byte[], int, int)
	 * @see   #bytesToIntBE(byte[], int, int, boolean)
	 */

	private static void intToBytesBE(
		int		value,
		byte[]	buffer,
		int		offset,
		int		length)
	{
		for (int i = offset + length - 1; i >= offset; i--)
		{
			buffer[i] = (byte)value;
			value >>= Byte.SIZE;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Converts an {@code int} to a little-endian sequence of bytes of the specified length.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @since 1.0
	 * @see   #intToBytesBE(int, byte[], int, int)
	 * @see   #bytesToIntLE(byte[], int, int, boolean)
	 */

	private static void intToBytesLE(
		int		value,
		byte[]	buffer,
		int		offset,
		int		length)
	{
		int endOffset = offset + length;
		for (int i = offset; i < endOffset; i++)
		{
			buffer[i] = (byte)value;
			value >>= Byte.SIZE;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Converts a big-endian sequence of bytes to a {@code long}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @return the {@code long} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #bytesToLongLE(byte[], int, int)
	 * @see    #longToBytesBE(long, byte[], int, int)
	 */

	private static long bytesToLongBE(
		byte[]	bytes,
		int		offset,
		int		length)
	{
		int endOffset = offset + length;
		long value = bytes[offset];
		while (++offset < endOffset)
		{
			value <<= Byte.SIZE;
			value |= bytes[offset] & 0xFF;
		}
		return value;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a little-endian sequence of bytes to a {@code long}.
	 *
	 * @param  bytes
	 *           the byte sequence that will be converted.
	 * @param  offset
	 *           the offset to {@code bytes} at which the sequence starts.
	 * @param  length
	 *           the length of the byte sequence.
	 * @return the {@code long} that results from the conversion of the byte sequence.
	 * @since  1.0
	 * @see    #bytesToLongBE(byte[], int, int)
	 * @see    #longToBytesLE(long, byte[], int, int)
	 */

	private static long bytesToLongLE(
		byte[]	bytes,
		int		offset,
		int		length)
	{
		int i = offset + length;
		long value = bytes[--i];
		while (--i >= offset)
		{
			value <<= Byte.SIZE;
			value |= bytes[i] & 0xFF;
		}
		return value;
	}

	//------------------------------------------------------------------

	/**
	 * Converts a {@code long} to a big-endian sequence of bytes of the specified length.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @since 1.0
	 * @see   #longToBytesLE(long, byte[], int, int)
	 * @see   #bytesToLongBE(byte[], int, int, boolean)
	 */

	private static void longToBytesBE(
		long	value,
		byte[]	buffer,
		int		offset,
		int		length)
	{
		for (int i = offset + length - 1; i >= offset; i--)
		{
			buffer[i] = (byte)value;
			value >>= Byte.SIZE;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Converts a {@code long} to a little-endian sequence of bytes of the specified length.
	 *
	 * @param value
	 *          the value that will be converted.
	 * @param buffer
	 *          the byte array that will contain the byte sequence.
	 * @param offset
	 *          the offset to {@code buffer} at which the sequence will start.
	 * @param length
	 *          the required length of the byte sequence.
	 * @since 1.0
	 * @see   #longToBytesBE(long, byte[], int, int)
	 * @see   #bytesToLongLE(byte[], int, int, boolean)
	 */

	private static void longToBytesLE(
		long	value,
		byte[]	buffer,
		int		offset,
		int		length)
	{
		int endOffset = offset + length;
		for (int i = offset; i < endOffset; i++)
		{
			buffer[i] = (byte)value;
			value >>= Byte.SIZE;
		}
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
