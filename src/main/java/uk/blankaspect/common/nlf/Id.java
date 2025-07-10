/*====================================================================*\

Id.java

Class: Nested-List File identifier.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE IDENTIFIER


/**
 * This class implements an identifier of a chunk of a Nested-List File.  An identifier is a string that is encoded as a
 * size byte followed by a UTF-8 sequence.  The size of the UTF-8 sequence must be between 1 and 255 bytes.
 * <p>
 * Identifiers that start with the '$' character (U+0024) are reserved.
 * </p>
 * <p>
 * To allow the conversion of a Nested-List File to XML, a non-reserved identifer must be a valid unprefixed name (ie, a
 * name that doesn't contain a ':') under XML 1.1.  Note that XML 1.1 names are less restrictive than those of XML 1.0,
 * so that a name that is valid under XML 1.1 might not be valid under XML 1.0.
 * </p>
 *
 * @since 1.0
 */

public class Id
	implements Comparable<Id>
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The size (in bytes) of the <i>size</i> field of an identifier. */
	public static final		int		SIZE_SIZE	= 1;

	/** The mask for the <i>size</i> field of an identifier. */
	protected static final	int		SIZE_MASK	= (1 << (SIZE_SIZE << 3)) - 1;

	/** The minimum size (in bytes) of an identifier. */
	public static final		int		MIN_SIZE	= 1;

	/** The maximum size (in bytes) of an identifier. */
	public static final		int		MAX_SIZE	= 255;

	/** The character that is prefixed to a reserved identifier. */
	public static final		char	RESERVED_PREFIX_CHAR	= '$';

	/** The string that is prefixed to a reserved identifier. */
	public static final		String	RESERVED_PREFIX			= Character.toString(RESERVED_PREFIX_CHAR);

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	value;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of an identifier from the specified array of bytes.  The bytes must be in the form in
	 * which an identifier appears in a Nested-List File: the first byte is the length of the UTF-8 sequence that
	 * follows.  The UTF-8 sequence is an encoding of the value of the identifier.
	 *
	 * @param  bytes
	 *           the array of bytes from which the identifier will be created.  The first element of the array is the
	 *           size of the UTF-8 sequence that follows.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>the first element of {@code bytes} is zero, or</li>
	 *             <li>the bytes starting at {@code bytes[1]} are not a valid UTF-8 sequence, or</li>
	 *             <li>the UTF-8 sequence, when decoded, is not a valid identifier.</li>
	 *           </ul>
	 * @since  1.0
	 */

	public Id(
		byte[]	bytes)
	{
		this(bytes, 0);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an identifier from the specified array of bytes, starting at the specified offset.  The
	 * bytes must be in the form in which an identifier appears in a Nested-List File: the byte at {@code offset} is the
	 * length of the UTF-8 sequence that follows.  The UTF-8 sequence is an encoding of the value of the identifier.
	 *
	 * @param  bytes
	 *           the array of bytes from which the identifier will be created.  The element of the array at {@code
	 *           offset} is the size of the UTF-8 sequence that follows.
	 * @param  offset
	 *           the offset to {@code bytes} at which the identifier begins.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>the element of {@code bytes} at {@code offset} is zero, or</li>
	 *             <li>the bytes starting at {@code bytes[offset+1]} are not a valid UTF-8 sequence, or</li>
	 *             <li>the UTF-8 sequence, when decoded, is not a valid identifier.</li>
	 *           </ul>
	 * @since  1.0
	 */

	public Id(
		byte[]	bytes,
		int		offset)
	{
		this(NlfUtils.utf8ToString(bytes, offset + SIZE_SIZE, getSize(bytes, offset)));
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an identifier from the specified string.
	 *
	 * @param  str
	 *           the string from which the identifier will be created.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>the string is empty, or</li>
	 *             <li>the UTF-8 encoding of the string is longer than 255 bytes, or</li>
	 *             <li>the string contains an invalid character.</li>
	 *           </ul>
	 * @since  1.0
	 */

	public Id(
		String	str)
	{
		// Validate string
		if (!isValidId(str))
			throw new IllegalArgumentException();

		// Set value
		value = str;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the size of an identifier that is encoded in the specified array of bytes, starting at the specified
	 * offset.
	 *
	 * @param  bytes
	 *           the array of bytes that contains the encoded identifier.
	 * @param  offset
	 *           the offset to {@code bytes} at which the identifier begins.
	 * @return the size of the encoded identifier (ie, the value of the element of {@code bytes} at {@code offset}).
	 * @since  1.0
	 */

	public static int getSize(
		byte[]	bytes,
		int		offset)
	{
		return (bytes[offset] & SIZE_MASK);
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} the specified string is a valid identifier.  A non-reserved identifier is valid if it is a
	 * valid unprefixed name (ie, a name that doesn't contain a ':') under XML 1.1.
	 *
	 * @param  str
	 *           the string that will be validated.
	 * @return {@code true} if {@code str} is a valid identifier; {@code false} otherwise.
	 * @since  1.0
	 */

	public static boolean isValidId(
		String	str)
	{
		if (!NlfUtils.isUtf8LengthWithinBounds(str, MIN_SIZE, MAX_SIZE))
			return false;

		int index = 0;
		while (index < str.length())
		{
			int codePoint = str.codePointAt(index);
			if (index == 0)
			{
				if (!NlfUtils.isNameStartChar(codePoint) && (codePoint != RESERVED_PREFIX_CHAR))
					return false;
			}
			else
			{
				if (!NlfUtils.isNameChar(codePoint))
					return false;
			}
			index += Character.charCount(codePoint);
		}
		return true;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : Comparable interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Compares this identifier with the specified identifier and returns the result.  The comparison is performed by
	 * comparing the values of the two identifiers using {@link String#compareTo(String)}, which compares strings
	 * lexicographically by the Unicode value of each character in the strings.
	 * <p>
	 * The result is
	 * </p>
	 * <ul>
	 *   <li>
	 *     a negative integer if the value of this identifier lexicographically precedes the value of the argument;
	 *   </li>
	 *   <li>
	 *     a positive integer if the value of this identifier lexicographically succeeds the value of the argument;
	 *   </li>
	 *   <li>
	 *     zero if the values of the two identifiers are equal.
	 *   </li>
	 * </ul>
	 * <p>
	 * A result of zero implies that the {@link #equals(Object)} method would return {@code true}.
	 * </p>
	 * <p>
	 * Because of the restrictions on the characters in an identifier, reserved identifiers will always precede
	 * non-reserved identifiers.
	 * </p>
	 *
	 * @param  id
	 *           the identifier with which this identifier will be compared.
	 * @return <ul>
	 *           <li>
	 *             {@code 0} (zero) if the value of this identifier is equal to the value of {@code id};
	 *           </li>
	 *           <li>
	 *             a value less than {@code 0} if the value of this identifier is lexicographically less than the value
	 *             of {@code id};
	 *           </li>
	 *           <li>
	 *             a value greater than {@code 0} if the value of this identifier is lexicographically greater than the
	 *             value of {@code id}.
	 *           </li>
	 *         </ul>
	 * @since  1.0
	 */

	@Override
	public int compareTo(
		Id	id)
	{
		return value.compareTo(id.value);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns {@code true} if specified object is an instance of {@code Id} that has the same value as this identifier.
	 *
	 * @param  obj
	 *           the object with which this identifier will be compared.
	 * @return {@code true} if {@code obj} is an instance of {@code Id} that has the same value as this identifier;
	 *         {@code false} otherwise.
	 * @since  1.0
	 */

	@Override
	public boolean equals(
		Object	obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof Id other) && value.equals(other.value);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the hash code of this identifier.
	 *
	 * @return the hash code of this identifier.
	 * @since  1.1
	 * @see    #equals(Object)
	 */

	@Override
	public int hashCode()
	{
		return value.hashCode();
	}

	//------------------------------------------------------------------

	/**
	 * Returns a string representation of this identifier.  The string is just the value of the identifier (ie, the
	 * value returned by {@link #getValue()}).
	 *
	 * @return a string representation of this identifier.
	 * @since  1.0
	 * @see    #getValue()
	 */

	@Override
	public String toString()
	{
		return value;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the value of this identifier.
	 *
	 * @return the value of this identifier.
	 * @since  1.0
	 */

	public String getValue()
	{
		return value;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of this identifier when it is encoded as a UTF-8 sequence.  The size does not include the
	 * initial size byte.
	 *
	 * @return the size of the UTF-8 encoding of this identifier.
	 * @since  1.0
	 * @see    #getFieldSize()
	 */

	public int getSize()
	{
		return NlfUtils.getUtf8Length(value);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of the field that the identifier would occupy in a Nested-List File.  The size includes the
	 * initial size byte and the UTF-8 encoding of the identifier.
	 *
	 * @return the size of the UTF-8 encoding of the identifier, including the initial size byte.
	 * @since  1.0
	 * @see    #getSize()
	 */

	public int getFieldSize()
	{
		return (SIZE_SIZE + getSize());
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this identifier is reserved (ie, it starts with the {@linkplain #RESERVED_PREFIX reserved
	 * prefix}).
	 *
	 * @return {@code true} if this identifier is reserved; {@code false} otherwise.
	 * @since  1.0
	 */

	public boolean isReserved()
	{
		return (value.startsWith(RESERVED_PREFIX));
	}

	//------------------------------------------------------------------

	/**
	 * Converts this identifier to a name, using the specified prefix, and returns the result.  The name is formed from
	 * the prefix and the value of the identifier: if the prefix is {@code null}, this method returns the string that is
	 * returned by {@link #getValue()}; otherwise, it returns a concatenation of the prefix, a vertical line character
	 * (U+007C) and the string that is returned by {@link #getValue()}.
	 *
	 * @param  prefix
	 *           the prefix that will be used to form the name.
	 * @return a string formed from {@code prefix} and the value of this identifier.
	 * @since  1.0
	 */

	public String toName(
		String	prefix)
	{
		return (prefix == null) ? value : prefix + NlfConstants.NAME_SEPARATOR_CHAR + value;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the encoded form of this identifier (a size byte followed by a UTF-8 sequence) as an array of bytes.
	 *
	 * @return a byte array containing the encoded form of the identifier: a size byte followed by a UTF-8 sequence.
	 * @since  1.0
	 */

	public byte[] getBytes()
	{
		byte[] bytes = NlfUtils.stringToUtf8(value);
		byte[] buffer = new byte[SIZE_SIZE + bytes.length];
		buffer[0] = (byte)bytes.length;
		System.arraycopy(bytes, 0, buffer, SIZE_SIZE, bytes.length);
		return buffer;
	}

	//------------------------------------------------------------------

	/**
	 * Writes the encoded form of this identifier (a size byte followed by a UTF-8 sequence) to the specified
	 * {@linkplain DataOutput data output}.
	 *
	 * @param  dataOutput
	 *           the data output to which the encoded form of the identifier (a size byte followed by a UTF-8 sequence)
	 *           will be written.
	 * @throws IOException
	 *           if an error occurs when writing this identifier to the data output.
	 * @see    #getBytes()
	 * @since  1.0
	 */

	public void write(
		DataOutput	dataOutput)
		throws IOException
	{
		dataOutput.write(getBytes());
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
