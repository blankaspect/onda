/*====================================================================*\

NlfUtils.java

Class: utility methods related to Nested-List Files.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.nio.ByteBuffer;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

//----------------------------------------------------------------------


// CLASS: UTILITY METHODS RELATED TO NESTED-LIST FILES


/**
 * This class contains publicly accessible utility methods that relate to Nested-List Files.
 *
 * @since 1.0
 */

public class NlfUtils
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Prevents this class from being instantiated externally.
	 */

	private NlfUtils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the specified UTF-8 sequence to a string.  An {@code IllegalArgumentException} is thrown if the UTF-8
	 * sequence is malformed or contains bytes that cannot be mapped to a character.  This method is equivalent to
	 * {@code #utf8ToString(data, 0, data.length)}.
	 *
	 * @param  data  an array that contains the UTF-8 sequence that will be decoded.
	 * @return the string that results from decoding the input sequence.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>the UTF-8 sequence is malformed, or</li>
	 *             <li>the UTF-8 sequence contains bytes that cannot be mapped to a character.</li>
	 *           </ul>
	 * @throws NlfUncheckedException
	 *           if the Java implementation does not support the UTF-8 character encoding, which is required of all Java
	 *           implementations.
	 * @since  1.0
	 * @see    #utf8ToString(byte[], int, int)
	 * @see    #stringToUtf8(String)
	 */

	public static String utf8ToString(byte[] data)
	{
		return utf8ToString(data, 0, data.length);
	}

	//------------------------------------------------------------------

	/**
	 * Decodes the specified UTF-8 sequence to a string.  An {@code IllegalArgumentException} is thrown if the UTF-8
	 * sequence is malformed or if it contains bytes that cannot be mapped to a character.
	 *
	 * @param  data    an array that contains the UTF-8 sequence that will be decoded.
	 * @param  offset  the offset to <b>{@code data}</b> at which the input sequence begins.
	 * @param  length  the length of the input sequence.
	 * @return the string that results from decoding the input sequence.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>the UTF-8 sequence is malformed, or</li>
	 *             <li>the UTF-8 sequence contains bytes that cannot be mapped to a character.</li>
	 *           </ul>
	 * @throws NlfUncheckedException
	 *           if the Java implementation does not support the UTF-8 character encoding, which is required of all Java
	 *           implementations.
	 * @since  1.0
	 * @see    #utf8ToString(byte[])
	 * @see    #stringToUtf8(String)
	 */

	public static String utf8ToString(byte[] data,
									  int    offset,
									  int    length)
	{
		try
		{
			CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
																	.onUnmappableCharacter(CodingErrorAction.REPORT);
			return decoder.decode(ByteBuffer.wrap(data, offset, length)).toString();
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException();
		}
	}

	//------------------------------------------------------------------

	/**
	 * Encodes the specified string as a UTF-8 sequence.
	 *
	 * @param  str  the string that will be encoded.
	 * @return an array containing the UTF-8 sequence that results from encoding the input string.
	 * @throws NlfUncheckedException
	 *           if the Java implementation does not support the UTF-8 character encoding, which is required of all Java
	 *           implementations.
	 * @since  1.0
	 * @see    #utf8ToString(byte[], int, int)
	 */

	public static byte[] stringToUtf8(String str)
	{
		return str.getBytes(StandardCharsets.UTF_8);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the length of the UTF-8 sequence that results from encoding the specified string.
	 *
	 * @param  str  the string whose encoded length is required.
	 * @return the length of the UTF-8 sequence that results from encoding the specified string.
	 * @since  1.0
	 * @see    #stringToUtf8(String)
	 */

	public static int getUtf8Length(String str)
	{
		return stringToUtf8(str).length;
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if the length of the UTF-8 sequence that results from encoding the specified string is
	 * within the specified bounds.
	 *
	 * @param  str        the string whose encoded length will be tested.
	 * @param  minLength  the minimum length of the encoded string.
	 * @param  maxLength  the maximum length of the encoded string.
	 * @return {@code true} if the length of the UTF-8 sequence that results from encoding <b>{@code str}</b> is within
	 *         the specified bounds; {@code false} otherwise.
	 * @since  1.0
	 * @see    #getUtf8Length(String)
	 */

	public static boolean isUtf8LengthWithinBounds(String str,
												   int    minLength,
												   int    maxLength)
	{
		int size = getUtf8Length(str);
		return ((size >= minLength) && (size <= maxLength));
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if the specified Unicode code point may be the first character of the identifier of a chunk
	 * or the name of an attribute.
	 * <p>
	 * The set of allowable characters is the same as that for XML names according to the <a
	 * href="http://www.w3.org/TR/xml11">XML 1.1 specification</a>, but without the ':' (colon) character.  Note that
	 * XML 1.1 names are less restrictive than those of XML 1.0, so that a name that is valid under XML 1.1 might not be
	 * valid under XML 1.0.
	 * </p>
	 *
	 * @param  codePoint  the Unicode code point that will be validated.
	 * @return {@code true} if <b>{@code codePoint}</b> is a valid first character of the identifier of a chunk or the
	 *         name of an attribute; {@code false} otherwise.
	 * @since  1.0
	 * @see    #isNameChar(int)
	 */

	public static boolean isNameStartChar(int codePoint)
	{
		return (((codePoint >= 'A') && (codePoint <= 'Z')) || (codePoint == '_')
				|| ((codePoint >= 'a') && (codePoint <= 'z'))
				|| ((codePoint >= 0xC0) && (codePoint <= 0xD6))
				|| ((codePoint >= 0xD8) && (codePoint <= 0xF6))
				|| ((codePoint >= 0xF8) && (codePoint <= 0x2FF))
				|| ((codePoint >= 0x370) && (codePoint <= 0x37D))
				|| ((codePoint >= 0x37F) && (codePoint <= 0x1FFF))
				|| ((codePoint >= 0x200C) && (codePoint <= 0x200D))
				|| ((codePoint >= 0x2070) && (codePoint <= 0x218F))
				|| ((codePoint >= 0x2C00) && (codePoint <= 0x2FEF))
				|| ((codePoint >= 0x3001) && (codePoint <= 0xD7FF))
				|| ((codePoint >= 0xF900) && (codePoint <= 0xFDCF))
				|| ((codePoint >= 0xFDF0) && (codePoint <= 0xFFFD))
				|| ((codePoint >= 0x10000) && (codePoint <= 0xEFFFF)));
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if the specified Unicode code point may be the second or subsequent character of the
	 * identifier of a chunk or the name of an attribute.
	 * <p>
	 * The set of allowable characters is the same as that for XML names according to the <a
	 * href="http://www.w3.org/TR/xml11">XML 1.1 specification</a>, but without the ':' (colon) character.  Note that
	 * XML 1.1 names are less restrictive than those of XML 1.0, so that a name that is valid under XML 1.1 might not be
	 * valid under XML 1.0.
	 * </p>
	 *
	 * @param  codePoint  the Unicode code point that will be validated.
	 * @return {@code true} if <b>{@code codePoint}</b> is a valid second or subsequent character of the identifier of a
	 *         chunk or the name of an attribute; {@code false} otherwise.
	 * @since  1.0
	 * @see    #isNameStartChar(int)
	 */

	public static boolean isNameChar(int codePoint)
	{
		return (isNameStartChar(codePoint) || (codePoint == '-') || (codePoint == '.')
				|| ((codePoint >= '0') && (codePoint <= '9')) || (codePoint == 0xB7)
				|| ((codePoint >= 0x0300) && (codePoint <= 0x036F))
				|| ((codePoint >= 0x203F) && (codePoint <= 0x2040)));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
