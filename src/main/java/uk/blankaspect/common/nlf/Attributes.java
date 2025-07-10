/*====================================================================*\

Attributes.java

Class: Nested-List File attributes chunk.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE ATTRIBUTES CHUNK


/**
 * This class implements a special {@linkplain Chunk chunk} that contains attributes.  A {@linkplain ChunkList chunk
 * list} may contain one attributes chunk, which has the reserved identifier {@code $ATTR} and consists of a list of
 * name&ndash;value pairs, analogous to the attributes of an element in an XML document.  The names of the attributes in
 * an attributes list must be unique.  In a Nested-List File, the name and value of an attribute are each encoded as two
 * size bytes followed by a UTF-8 sequence.  The byte order of the size bytes is determined by the byte-order flag in
 * the header of the Nested-List File {@link Document document} to which the chunk belongs.  The maximum size of a name
 * or value is 65535 bytes.
 * <p>
 * To allow the conversion of a Nested-List File to XML, an attribute name must be valid unprefixed name (ie, a name
 * that doesn't contain a ':') under XML 1.1.  Note that XML 1.1 names are less restrictive than those of XML 1.0, so
 * that a name that is valid under XML 1.1 might not be valid under XML 1.0.
 * </p>
 * <p>
 * When an attributes chunk is constructed, its chunk writer is set to a default writer, which is suitable for use by
 * {@link Document#write(File)}.  The default writer may be replaced, using {@link Chunk#setWriter(Chunk.IWriter)}, if
 * it is unsuitable for a particular purpose.  The <i>rewrite</i> flag, which is accessed with {@link #isRewrite()} and
 * {@link #setRewrite(boolean)}, is used by the default writer to indicate to the {@linkplain Document#write(File)
 * document writer} whether the attributes chunk should be rewritten on the second pass (for example, if an attribute
 * value can be set only after subsequent chunks have been written).
 * </p>
 *
 * @since 1.0
 * @see   Chunk
 * @see   ChunkList
 */

public class Attributes
	extends Chunk
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The identifier of an {@linkplain Attributes attributes chunk}. */
	public static final		Id	ATTRIBUTES_ID	= new Id(Id.RESERVED_PREFIX + "ATTR");

	/** The size (in bytes) of the <i>name size</i> field of an attribute. */
	public static final		int	NAME_SIZE_SIZE	= 2;

	/** The mask for the <i>name size</i> field of an attribute. */
	protected static final	int	NAME_SIZE_MASK	= (1 << (NAME_SIZE_SIZE << 3)) - 1;

	/** The minimum size (in bytes) of the name of an attribute. */
	public static final		int	MIN_NAME_SIZE	= 1;

	/** The maximum size (in bytes) of the name of an attribute. */
	public static final		int	MAX_NAME_SIZE	= (1 << 16) - 1;

	/** The size (in bytes) of the <i>value size</i> field of an attribute. */
	public static final		int	VALUE_SIZE_SIZE	= 2;

	/** The mask for the <i>value size</i> field of an attribute. */
	protected static final	int	VALUE_SIZE_MASK	= (1 << (VALUE_SIZE_SIZE << 3)) - 1;

	/** The minimum size (in bytes) of the value of an attribute. */
	public static final		int	MIN_VALUE_SIZE	= 0;

	/** The maximum size (in bytes) of the value of an attribute. */
	public static final		int	MAX_VALUE_SIZE	= (1 << 16) - 1;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<Attr>	attributes;
	private	boolean		rewrite;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of an attributes chunk.  An attributes chunk can be created only by the {@linkplain
	 * ChunkList list} to which it belongs.  A list can contain no more than one attributes chunk.
	 * <p>
	 * The chunk writer for the attributes chunk is set to a default writer, which may be replaced by calling {@link
	 * Chunk#setWriter(Chunk.IWriter)}.
	 * </p>
	 *
	 * @param document
	 *          the document to which the attributes chunk will belong.
	 * @since 1.0
	 */

	protected Attributes(
		Document	document)
	{
		// Call superclass constructor
		super(document, ATTRIBUTES_ID);

		// Initialise instance variables
		setSize(0);
		setWriter(new Writer());
		attributes = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns {@code true} if the specified string is a valid attribute name.  An attribute name is valid if it is a
	 * valid unprefixed name (ie, a name that doesn't contain a ':') under XML 1.1.
	 *
	 * @param  str
	 *           the string whose validity will be determined.
	 * @return {@code true} if {@code str} is a valid attribute name; {@code false} otherwise.
	 * @since  1.0
	 */

	public static boolean isValidName(
		String	str)
	{
		if (!NlfUtils.isUtf8LengthWithinBounds(str, MIN_NAME_SIZE, MAX_NAME_SIZE))
			return false;

		int index = 0;
		while (index < str.length())
		{
			int codePoint = str.codePointAt(index);
			if (index == 0)
			{
				if (!NlfUtils.isNameStartChar(codePoint))
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
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns {@code true} if specified object is an instance of {@code Attributes} that contains the same attributes
	 * as this attributes chunk, though not necessarily in the same order.
	 *
	 * @param  obj
	 *           the object with which this attributes chunk will be compared.
	 * @return {@code true} if {@code obj} is an instance of {@code Attributes} that contains the same attributes as
	 *         this attributes chunk, though not necessarily in the same order; {@code false} otherwise.
	 * @since  1.0
	 */

	@Override
	public boolean equals(
		Object	obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof Attributes other) && (attributes.size() == other.attributes.size())
				&& attributes.stream().allMatch(attr -> other.attributes.contains(attr));
	}

	//------------------------------------------------------------------

	/**
	 * Returns the hash code of this attributes chunk.
	 *
	 * @return the hash code of this attributes chunk.
	 * @since  1.1
	 * @see    #equals(Object)
	 */

	@Override
	public int hashCode()
	{
		Attr[] attrArray = attributes.toArray(Attr[]::new);
		Arrays.sort(attrArray);
		return Arrays.hashCode(attrArray);
	}

	//------------------------------------------------------------------

	/**
	 * Returns a string representation of this attributes chunk.  Each attribute in the attributes list has the form
	 * <i>name</i>="<i>value</i>", and attributes are separated with a comma and a space.
	 *
	 * @return a string representation of this attributes chunk, in which each attribute has the form
	 *         <i>name</i>="<i>value</i>", and attributes are separated with a comma and a space.
	 * @since  1.0
	 * @see    Attr#toString()
	 */

	@Override
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < attributes.size(); i++)
		{
			if (i > 0)
				buffer.append(", ");
			buffer.append(attributes.get(i));
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns {@code true} if the attributes of this attributes chunk will be rewritten on the second pass of the
	 * document writer.
	 *
	 * @return {@code true} if the attributes of this chunk will be rewritten on the second pass of the document writer;
	 *         {@code false} otherwise.
	 * @since  1.0
	 * @see    #setRewrite(boolean)
	 */

	public boolean isRewrite()
	{
		return rewrite;
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this attributes chunk contains at least one attribute.
	 *
	 * @return {@code true} if this attributes chunk contains at least one attribute; {@code false} otherwise.
	 * @since  1.0
	 * @see    #getNumAttributes()
	 */

	public boolean hasAttributes()
	{
		return !attributes.isEmpty();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the number of attributes in this chunk's list of attributes.
	 *
	 * @return the number of attributes in this chunk's list of attributes.
	 * @since  1.0
	 * @see    #hasAttributes()
	 */

	public int getNumAttributes()
	{
		return attributes.size();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the attribute at the specified index in this chunk's list of attributes.
	 *
	 * @param  index
	 *           the index of the required attribute in this chunk's list of attributes.
	 * @return the attribute at {@code index} in this chunk's list of attributes.
	 * @throws IndexOutOfBoundsException
	 *           if {@code (index < 0)} or {@code (index >= }{@link #getNumAttributes()}{@code)}.
	 * @since  1.0
	 * @see    #getNumAttributes()
	 */

	public Attr getAttribute(
		int	index)
	{
		return attributes.get(index);
	}

	//------------------------------------------------------------------

	/**
	 * Sets the flag that indicates whether the attributes will be rewritten on the second pass of the document writer.
	 * The flag, which is used by an {@code Attributes} object's default writer, can be tested by a replacement writer
	 * with {@link #isRewrite()}.
	 *
	 * @param rewrite
	 *          {@code true} if the attributes should be rewritten on the second pass of the document writer; {@code
	 *          false} otherwise.
	 * @since 1.0
	 * @see   #isRewrite()
	 */

	public void setRewrite(
		boolean	rewrite)
	{
		this.rewrite = rewrite;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the index of the attribute with the specified name in this chunk's list of attributes.  As the attributes
	 * in the list must have unique names, any occurrence in the list of an attribute with a given name will be the only
	 * occurrence.
	 *
	 * @param  name
	 *           the name of the attribute whose index is required.
	 * @return the index of the attribute whose name is {@code name} in this chunk's list of attributes, or {@code -1}
	 *         if the list does not contain such an attribute.
	 * @throws IllegalArgumentException
	 *           if {@code name} is {@code null}.
	 * @since  1.0
	 */

	public int indexOf(
		String	name)
	{
		if (name == null)
			throw new IllegalArgumentException();

		for (int i = 0; i < attributes.size(); i++)
		{
			if (attributes.get(i).name.equals(name))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the specified attribute in this chunk's list of attributes.  If the list already contains an attribute with
	 * the same name, it is replaced with the specified attribute; otherwise, the new attribute is added to the end of
	 * the list.
	 *
	 * @param  attribute
	 *           the attribute that will be set in or added to the list of attributes.
	 * @throws IllegalArgumentException
	 *           if {@code attribute} is {@code null}.
	 * @since  1.0
	 * @see    #removeAttribute(int)
	 */

	public void setAttribute(
		Attr	attribute)
	{
		// Validate argument
		if (attribute == null)
			throw new IllegalArgumentException();

		// Get index to list
		int index = indexOf(attribute.name);

		// Add attribute to list
		if (index < 0)
			attributes.add(attribute);

		// Replace attribute in list
		else
		{
			decrementSize(attributes.get(index).getSize());
			attributes.set(index, attribute);
		}

		// Increment chunk size
		incrementSize(attribute.getSize());
	}

	//------------------------------------------------------------------

	/**
	 * Removes the attribute at the specified index in this chunk's list of attributes, and returns the attribute that
	 * was removed.
	 *
	 * @param  index
	 *           the index of the attribute that will be removed from this chunk's list of attributes.
	 * @return the attribute that was removed from the list.
	 * @throws IndexOutOfBoundsException
	 *           if {@code (index < 0)} or {@code (index >= }{@link #getNumAttributes()}{@code)}.
	 * @since  1.0
	 * @see    #setAttribute(Attr)
	 * @see    #getNumAttributes()
	 * @see    #indexOf(String)
	 */

	public Attr removeAttribute(
		int	index)
	{
		Attr attr = attributes.remove(index);
		decrementSize(attr.getSize());
		return attr;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the attributes in this chunk's list of attributes as attributes of the specified XML element.
	 *
	 * @param  element
	 *           the XML element on which the attributes will be set.
	 * @throws DOMException
	 *           if an attribute name is not valid for the XML version of the document to which {@code element} belongs.
	 * @since  1.0
	 */

	protected void toXml(
		Element	element)
		throws DOMException
	{
		for (Attr attribute : attributes)
			element.setAttribute(attribute.name, attribute.value);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: ATTRIBUTE NAME-VALUE PAIR


	/**
	 * This class implements an attribute as a name&ndash;value pair.  An {@linkplain Attributes attributes chunk}
	 * consists of a list of name&ndash;value pairs.
	 *
	 * @since 1.0
	 */

	public static class Attr
		implements Comparable<Attr>
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	name;
		private	String	value;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an attribute with the specified name.  The value of the attribute is an empty
		 * string.
		 *
		 * @param  name
		 *           the name of the attribute.
		 * @throws IllegalArgumentException
		 *           if
		 *           <ul>
		 *             <li>{@code name} is {@code null}, or</li>
		 *             <li>the length of the UTF-8 encoding of {@code name} is greater than 65535 bytes, or</li>
		 *             <li>{@code name} is not a valid attribute name.</li>
		 *           </ul>
		 * @since  1.0
		 */

		public Attr(
			String	name)
		{
			this(name, "");
		}

		//--------------------------------------------------------------

		/**
		 * Creates a new instance of an attribute with the specified name and value.
		 *
		 * @param  name
		 *           the attribute name.
		 * @param  value
		 *           the attribute value.
		 * @throws IllegalArgumentException
		 *           if
		 *           <ul>
		 *             <li>@code name} is {@code null}, or</li>
		 *             <li>the length of the UTF-8 encoding of {@code name} is greater than 65535 bytes, or</li>
		 *             <li>@code name} is not a valid attribute name, or</li>
		 *             <li>{@code value} is {@code null}, or</li>
		 *             <li>the length of the UTF-8 encoding of {@code value} is greater than 65535 bytes.</li>
		 *           </ul>
		 * @since  1.0
		 */

		public Attr(
			String	name,
			String	value)
		{
			// Validate arguments
			if (name == null)
				throw new IllegalArgumentException("Null name");
			if (!isValidName(name))
				throw new IllegalArgumentException("Invalid name");
			if (value == null)
				throw new IllegalArgumentException("Null value");
			if (!NlfUtils.isUtf8LengthWithinBounds(value, MIN_VALUE_SIZE, MAX_VALUE_SIZE))
				throw new IllegalArgumentException("Length of value out of bounds");

			// Initialise instance variables
			this.name = name;
			this.value = value;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Comparable interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Compares this attribute with the specified attribute and returns the result.  The comparison is performed by
		 * comparing the names of the two objects using {@link String#compareTo(String)}, which compares strings
		 * lexicographically by the Unicode value of each character in the strings.
		 * <p>
		 * The result is
		 * </p>
		 * <ul>
		 *   <li>
		 *     a negative integer if the name of this attribute lexicographically precedes the name of the argument;
		 *   </li>
		 *   <li>
		 *     a positive integer if the name of this attribute lexicographically succeeds the name of the argument;
		 *   </li>
		 *   <li>
		 *     zero if the names of the two attributes are equal.
		 *   </li>
		 * </ul>
		 * <p>
		 * Note that a result of zero <em>does not</em> imply that the {@link #equals(Object)} method would return
		 * {@code true}, although the converse is true.
		 * </p>
		 *
		 * @param  attr
		 *           the attribute with which this attribute will be compared.
		 * @return <ul>
		 *           <li>
		 *             {@code 0} (zero) if the name of this attribute is equal to the name of {@code attr};
		 *           </li>
		 *           <li>
		 *             a value less than {@code 0} if the name of this attribute is lexicographically less than the name
		 *             of {@code attr};
		 *           </li>
		 *           <li>
		 *             a value greater than {@code 0} if the name of this attribute is lexicographically greater than
		 *             the name of {@code attr}.
		 *           </li>
		 *         </ul>
		 * @since  1.0
		 * @see    String#compareTo(String)
		 */

		@Override
		public int compareTo(
			Attr	attr)
		{
			return name.compareTo(attr.name);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns {@code true} if specified object is an instance of {@code Attr} that has the same name and value as
		 * this attribute.
		 *
		 * @param  obj
		 *           the object with which this attribute will be compared.
		 * @return {@code true} if {@code obj} is an instance of {@code Attr} that has the same name and value as this
		 *         attribute; {@code false} otherwise.
		 * @since  1.0
		 */

		@Override
		public boolean equals(
			Object	obj)
		{
			if (this == obj)
				return true;

			return (obj instanceof Attr other) && name.equals(other.name) && value.equals(other.value);
		}

		//--------------------------------------------------------------

		/**
		 * Returns the hash code of this attribute.
		 *
		 * @return the hash code of this attribute.
		 * @since  1.1
		 * @see    #equals(Object)
		 */

		@Override
		public int hashCode()
		{
			return 31 * name.hashCode() + value.hashCode();
		}

		//--------------------------------------------------------------

		/**
		 * Returns a string representation of this attribute.  The string is in the form <i>name</i>="<i>value</i>".
		 *
		 * @return a string representation of this attribute, in the form <i>name</i>="<i>value</i>".
		 * @since  1.0
		 */

		@Override
		public String toString()
		{
			return name + "=\"" + value + "\"";
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the name of this attribute.
		 *
		 * @return the name of this attribute.
		 * @since  1.0
		 */

		public String getName()
		{
			return name;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the value of this attribute.
		 *
		 * @return the value of this attribute.
		 * @since  1.0
		 */

		public String getValue()
		{
			return value;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the size of the encoded form of this attribute (ie, the form in which it is encoded in an attributes
		 * chunk in a Nested-List File).  The name and value of the attribute are each encoded as two size bytes
		 * followed by a UTF-8 sequence.
		 *
		 * @return the size of the encoded form of this attribute.
		 * @since  1.0
		 * @see    #getBytes(boolean)
		 */

		public int getSize()
		{
			return (NAME_SIZE_SIZE + NlfUtils.getUtf8Length(name) + VALUE_SIZE_SIZE + NlfUtils.getUtf8Length(value));
		}

		//--------------------------------------------------------------

		/**
		 * Returns the encoded form of this attribute (ie, the form in which it is encoded in an attributes chunk in a
		 * Nested-List File).  The name and value of the attribute are each encoded as two size bytes followed by a
		 * UTF-8 sequence.
		 *
		 * @param  littleEndian
		 *           {@code true} if the byte order of the size of the name and value is little-endian; {@code false} if
		 *           the byte order of the size of the name and value is big-endian.
		 * @return the encoded form of this attribute.
		 * @since  1.0
		 * @see    #getSize()
		 */

		public byte[] getBytes(
			boolean	littleEndian)
		{
			// Encode the name of this attribute and its size
			byte[] nameBytes = NlfUtils.stringToUtf8(name);
			byte[] nameSizeBuffer = new byte[NAME_SIZE_SIZE];
			Utils.intToBytes(nameBytes.length, nameSizeBuffer, 0, nameSizeBuffer.length, littleEndian);

			// Encode the value of this attribute and its size
			byte[] valueBytes = NlfUtils.stringToUtf8(value);
			byte[] valueSizeBuffer = new byte[VALUE_SIZE_SIZE];
			Utils.intToBytes(valueBytes.length, valueSizeBuffer, 0, valueSizeBuffer.length, littleEndian);

			// Concatenate the size of the name, the name, the size of the value and the value
			int size = NAME_SIZE_SIZE + nameBytes.length + VALUE_SIZE_SIZE + valueBytes.length;
			byte[] buffer = new byte[size];
			int offset = 0;
			System.arraycopy(nameSizeBuffer, 0, buffer, offset, nameSizeBuffer.length);
			offset += nameSizeBuffer.length;
			System.arraycopy(nameBytes, 0, buffer, offset, nameBytes.length);
			offset += nameBytes.length;
			System.arraycopy(valueSizeBuffer, 0, buffer, offset, valueSizeBuffer.length);
			offset += valueSizeBuffer.length;
			System.arraycopy(valueBytes, 0, buffer, offset, valueBytes.length);

			// Return encoded attribute
			return buffer;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: ATTRIBUTES WRITER


	/**
	 * This class implements a {@linkplain Chunk.IWriter chunk writer}, with which the list of attributes of the
	 * enclosing {@linkplain Attributes attributes chunk} is written to a Nested-List File.  An instance of this class
	 * is set as the default writer when the attributes chunk is created.
	 *
	 * @since 1.0
	 */

	private class Writer
		implements Chunk.IWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of an attributes writer, which implements {@link Chunk.IWriter}.
		 *
		 * @since 1.0
		 */

		private Writer()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Chunk.IWriter interface
	////////////////////////////////////////////////////////////////////

		/**
		 * This method, which is called before a chunk is written, performs no action.  It returns the value of the
		 * {@link Attributes#rewrite} field to indicate whether the chunk should be rewritten on the second pass of the
		 * document writer.
		 *
		 * @param  pass
		 *           the index of the pass (0 = first pass, 1 = second pass) of the document writer that calls this
		 *           method.
		 * @return {@code true} if the chunk should be rewritten on the second pass of the document writer; {@code
		 *         false} otherwise.
		 * @since  1.0
		 * @see    Chunk.IWriter#reset(int)
		 * @see    Document#write(File)
		 * @see    Attributes#rewrite
		 */

		@Override
		public boolean reset(
			int	pass)
		{
			return (pass == 0) ? rewrite : false;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the size of the enclosing attributes chunk.
		 *
		 * @return the size of the enclosing attributes chunk.
		 * @since  1.0
		 * @see    Chunk.IWriter#getLength(int)
		 * @see    Document#write(File)
		 */

		@Override
		public long getLength()
		{
			long size = 0;
			for (Attr attr : attributes)
				size += attr.getSize();
			return size;
		}

		//--------------------------------------------------------------

		/**
		 * Writes the attributes chunk to the specified output.
		 *
		 * @param  dataOutput
		 *           the {@code DataOutput} object to which the attributes chunk will be written.
		 * @throws IOException
		 *           if an I/O error occurs.
		 * @since  1.0
		 * @see    Chunk.IWriter#write(DataOutput)
		 * @see    Document#write(File)
		 */

		@Override
		public void write(
			DataOutput	dataOutput)
			throws IOException
		{
			for (Attr attr : attributes)
				dataOutput.write(attr.getBytes(getDocument().isLittleEndian()));
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
