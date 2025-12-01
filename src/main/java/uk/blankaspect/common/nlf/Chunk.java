/*====================================================================*\

Chunk.java

Class: Nested-List File chunk.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE CHUNK


/**
 * This class implements a chunk of a Nested-List File.  The chunk, which is the primary structural unit of a
 * Nested-List File, consists of a header and binary data.  The header consists of an {@linkplain Id identifier} and a
 * <i>size</i> field.  The <i>identifier</i> field is of variable size, up to 256 bytes; the size of the <i>size</i>
 * field is 8 bytes.  The chunk size does not include the size of the header.
 * <p>
 * A chunk is created with {@link Document#createChunk(Id)}, and is added to a list with {@link
 * ChunkList#appendChunk(Chunk)}.  A chunk can be added only to a list that belongs to the same document as the chunk.
 * </p>
 * <p>
 * There are two special chunks: the {@linkplain ChunkList list} and the {@linkplain Attributes attributes chunk}, which
 * have the reserved identifiers {@code "$LIST"} and {@code "$ATTR"} respectively.
 * </p>
 *
 * @since 1.0
 * @see   ChunkList
 */

public class Chunk
	implements Comparable<Chunk>
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The size (in bytes) of the <i>size</i> field of a chunk. */
	public static final	int		SIZE_SIZE	= 8;

	/** The minimum size (in bytes) of a chunk. */
	public static final	long	MIN_SIZE	= 0;

	/** The maximum size (in bytes) of a chunk. */
	public static final	long	MAX_SIZE	= (1L << 62) - 1;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Document	document;
	private	ChunkList	parent;
	private	Id			id;
	private	long		size;
	private	IReader		reader;
	private	IWriter		writer;
	private	IEncoder	encoder;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a chunk with the specified owner document and identifier.  A Base64 encoder is set as
	 * the default encoder.
	 *
	 * @param document
	 *          the {@linkplain Document document} to which the chunk will belong.
	 * @param id
	 *          the chunk identifier.
	 * @since 1.0
	 */

	protected Chunk(
		Document	document,
		Id			id)
	{
		// Initialise instance variables
		this.document = document;
		this.id = id;
		size = -1;
		if (!id.isReserved())
			encoder = Base64Encoder.INSTANCE;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : Comparable interface
////////////////////////////////////////////////////////////////////////

	/**
	 * Compares this chunk with the specified chunk and returns the result.  The comparison is performed by comparing
	 * the identifiers of the two objects using {@link String#compareTo(String)}, which compares strings
	 * lexicographically by the Unicode value of each character in the strings.
	 * <p>
	 * The result is
	 * </p>
	 * <ul>
	 *   <li>
	 *     a negative integer if the identifier of this chunk lexicographically precedes the identifier of the argument;
	 *   </li>
	 *   <li>
	 *     a positive integer if the identifier of this chunk lexicographically succeeds the identifier of the argument;
	 *   </li>
	 *   <li>
	 *     zero if the identifiers of the two chunks are equal.
	 *   </li>
	 * </ul>
	 * <p>
	 * Because of the restrictions on identifiers, special chunks (ie, chunks with reserved identifiers) will always
	 * precede general chunks (ie, chunks with non-reserved identifiers).
	 * </p>
	 *
	 * @param  chunk
	 *           the chunk with which this chunk will be compared.
	 * @return <ul>
	 *           <li>
	 *             {@code 0} (zero) if the identifier of this chunk is equal to the identifier of {@code chunk};
	 *           </li>
	 *           <li>
	 *             a value less than {@code 0} if the identifier of this chunk is lexicographically less than the
	 *             identifier of {@code chunk};
	 *           </li>
	 *           <li>
	 *             a value greater than {@code 0} if the identifier of this chunk is lexicographically greater than the
	 *             identifier of {@code chunk}.
	 *           </li>
	 *         </ul>
	 * @since  1.0
	 * @see    Id#compareTo(Id)
	 */

	@Override
	public int compareTo(
		Chunk	chunk)
	{
		return id.compareTo(chunk.id);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the Nested-List File document to which this chunk belongs.
	 *
	 * @return the document to which this chunk belongs.
	 * @since  1.0
	 */

	public Document getDocument()
	{
		return document;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the chunk list that contains this chunk, or {@code null} if this chunk has no parent.
	 *
	 * @return the chunk {@linkplain ChunkList list} that contains this chunk, or {@code null} if this chunk has no
	 *         parent.
	 * @since  1.0
	 * @see    #setParent(ChunkList)
	 */

	public ChunkList getParent()
	{
		return parent;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the identifier of this chunk.
	 *
	 * @return the identifier of this chunk.
	 * @since  1.0
	 */

	public Id getId()
	{
		return id;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of this chunk in bytes.  A value of {@code -1} indicates that the size is invalid and that the
	 * size of the chunk should be obtained by another means (eg, from {@link #getWriter()}{@link IWriter#getLength()
	 * .getLength()}).
	 *
	 * @return the size of this chunk.
	 * @since  1.0
	 * @see    #setSize(long)
	 * @see    #updateSize()
	 * @see    IWriter#getLength()
	 */

	public long getSize()
	{
		return size;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the chunk reader for this chunk.  The reader is used to read the chunk data from a data input, usually a
	 * Nested-List File document.
	 *
	 * @return the {@linkplain IReader chunk reader} that reads the data of this chunk.
	 * @since  1.0
	 * @see    #setReader(IReader)
	 * @see    IReader
	 */

	public IReader getReader()
	{
		return reader;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the chunk writer for this chunk.  The writer is used to write the chunk data to a data output, usually a
	 * Nested-List File document.
	 *
	 * @return the {@linkplain IWriter chunk writer} that writes the data of this chunk.
	 * @since  1.0
	 * @see    #setWriter(IWriter)
	 * @see    IWriter
	 */

	public IWriter getWriter()
	{
		return writer;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the chunk encoder for this chunk.  The encoder is used by {@link #toXml(org.w3c.dom.Document)} to encode
	 * the chunk data for use as a text node of an XML file.
	 *
	 * @return the {@linkplain IEncoder chunk encoder} that encodes the data of this chunk.
	 * @since  1.0
	 * @see    #setEncoder(IEncoder)
	 * @see    IEncoder
	 */

	public IEncoder getEncoder()
	{
		return encoder;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the size of this chunk to the specified value.
	 *
	 * @param size  the value to which the size of this chunk will be set.
	 * @since 1.0
	 * @see   #getSize()
	 * @see   #updateSize()
	 */

	public void setSize(
		long	size)
	{
		this.size = size;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the chunk reader for this chunk to the specified value.  The reader is used to read the chunk data from a
	 * data input, usually a Nested-List File document.
	 *
	 * @param reader  the {@link IReader} that will be set as this chunk's reader.
	 * @since 1.0
	 * @see   #getReader()
	 */

	public void setReader(
		IReader	reader)
	{
		this.reader = reader;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the chunk writer for this chunk to the specified value.  The writer is used to write the chunk data to a
	 * data output, usually a Nested-List File document.
	 *
	 * @param writer  the {@link IWriter} that will be set as this chunk's writer.
	 * @since 1.0
	 * @see   #getWriter()
	 */

	public void setWriter(
		IWriter	writer)
	{
		this.writer = writer;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the chunk encoder for this chunk to the specified value.  The encoder is used by {@link
	 * #toXml(org.w3c.dom.Document)} to encode the chunk data for use as a text node of an XML file.
	 *
	 * @param encoder  the {@link IEncoder} that will be set as this chunk's encoder.
	 * @since 1.0
	 * @see   #getEncoder()
	 */

	public void setEncoder(
		IEncoder	encoder)
	{
		this.encoder = encoder;
	}

	//------------------------------------------------------------------

	/**
	 * Tests whether this chunk is an {@linkplain Attributes attributes chunk} or a subclass of an attributes chunk.
	 *
	 * @return {@code true} if this chunk is an attributes chunk or a subclass of an attributes chunk; {@code false}
	 *         otherwise.
	 * @since  1.0
	 */

	public boolean isAttributes()
	{
		return (this instanceof Attributes);
	}

	//------------------------------------------------------------------

	/**
	 * Tests whether this chunk is a {@linkplain ChunkList chunk list} or a subclass of a chunk list.
	 *
	 * @return {@code true} if this chunk is a chunk list or a subclass of a chunk list; {@code false} otherwise.
	 * @since  1.0
	 */

	public boolean isList()
	{
		return (this instanceof ChunkList);
	}

	//------------------------------------------------------------------

	/**
	 * Adds the specified value to the size of this chunk.
	 *
	 * @param increment
	 *          the value that will be added to the size of this chunk.
	 * @since 1.6
	 * @see   #decrementSize(long)
	 */

	public void incrementSize(
		long	increment)
	{
		size += increment;
	}

	//------------------------------------------------------------------

	/**
	 * Subtracts the specified value from the size of this chunk.
	 *
	 * @param decrement
	 *          the value that will be subtracted from the size of this chunk.
	 * @since 1.6
	 * @see   #incrementSize(long)
	 */

	public void decrementSize(
		long	decrement)
	{
		size -= decrement;
	}

	//------------------------------------------------------------------

	/**
	 * Updates the chunk's <i>size</i> field from the chunk writer, if one has been set.  The chunk size is set to the
	 * value returned by the chunk writer's {@link IWriter#getLength() getLength()} method.
	 *
	 * @since 1.0
	 * @see   #getSize()
	 * @see   #setSize(long)
	 * @see   IWriter#getLength()
	 */

	public void updateSize()
	{
		if (writer != null)
			size = writer.getLength();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the namespace name that applies to this chunk.  A chunk's namespace name is the first non-{@code null}
	 * local namespace name that is encountered in an ancestor when ascending the document tree.  If none of the chunk's
	 * ancestors has a local namespace name, {@code null} is returned.
	 *
	 * @return the namespace name that applies to this chunk, or {@code null} if no namespace name applies.
	 * @since  1.0
	 * @see    ChunkList#getNamespaceName()
	 */

	public String getNamespaceName()
	{
		String namespaceName = null;
		ChunkList list = parent;
		while (list != null)
		{
			namespaceName = list.getLocalNamespaceName();
			if (namespaceName != null)
				break;
			list = list.getParent();
		}
		return namespaceName;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the name of this chunk.  A chunk's name is formed by prefixing its namespace name and a vertical line
	 * character (U+007C) to its identifier.  If the chunk has no namespace name, its name is just its identifier.
	 *
	 * @return the name of this chunk.
	 * @since  1.0
	 * @see    ChunkList#getName()
	 */

	public String getName()
	{
		return id.toName(getNamespaceName());
	}

	//------------------------------------------------------------------

	/**
	 * Returns the pathname of this chunk.  A pathname, which might not be unique within a particular document, is
	 * formed by concatenating the list-instance identifiers of the ancestors of this chunk with the identifier of this
	 * chunk.  The components of the pathname (ie, the identifiers) are separated with '.' (U+002E).  The pathname has a
	 * leading '.' if its first component is not the root list.
	 *
	 * @return the pathname of this chunk.
	 * @since  1.0
	 */

	public String getPathname()
	{
		String prefix = (parent == null) ? "" : parent.getPathname();
		return prefix + NlfConstants.PATHNAME_SEPARATOR_CHAR + id.getValue();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of the header (identifier and size) of this chunk.
	 *
	 * @return the size of the header (identifier and size) of this chunk.
	 * @since  1.0
	 */

	public int getHeaderSize()
	{
		return id.getFieldSize() + SIZE_SIZE;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of this chunk as an array of bytes, with the specified byte order.
	 *
	 * @param  littleEndian
	 *           {@code true} if the byte order is little-endian; {@code false} if the byte order is big-endian.
	 * @return the size of this chunk as an array of bytes.
	 * @since  1.0
	 */

	public byte[] getSizeBytes(
		boolean	littleEndian)
	{
		byte[] buffer = new byte[SIZE_SIZE];
		Utils.longToBytes(size, buffer, 0, buffer.length, littleEndian);
		return buffer;
	}

	//------------------------------------------------------------------

	/**
	 * Writes the header (identifier and size) of this chunk to the specified data output.
	 *
	 * @param  dataOutput
	 *           the data output to which the header of this chunk will be written.
	 * @throws IOException
	 *           if an error occurs when writing the header to the data output.
	 * @since  1.0
	 */

	public void writeHeader(
		DataOutput	dataOutput)
		throws IOException
	{
		// Write identifier
		id.write(dataOutput);

		// Write size
		dataOutput.write(getSizeBytes(document.isLittleEndian()));
	}

	//------------------------------------------------------------------

	/**
	 * Generates an XML element of the specified XML document from this chunk and returns the result.  The chunk data is
	 * obtained from the chunk's reader, and encoded as text with the chunk's encoder.
	 *
	 * @param  xmlDocument
	 *           the XML document that will be the owner of the element that is created.
	 * @return the XML element that is generated from this chunk.
	 * @throws DOMException
	 *           if an exception occurs when creating the XML element.
	 * @throws IOException
	 *           if an I/O error occurs when reading the chunk data.
	 * @throws NlfException
	 *           if
	 *           <ul>
	 *             <li>an exception occurs when encoding the chunk data, or</li>
	 *             <li>there is not enough memory to create an XML element.</li>
	 *           </ul>
	 * @since  1.0
	 */

	public Element toXml(
		org.w3c.dom.Document	xmlDocument)
		throws DOMException, IOException, NlfException
	{
		final	int	DEFAULT_INPUT_LENGTH	= 1024;

		Element element = xmlDocument.createElement(id.getValue());
		if ((size > 0) && (reader != null) && (encoder != null))
		{
			try
			{
				// Reset reader and encoder
				reader.reset();
				encoder.reset();

				// Allocate buffer for block of chunk data
				int inLength = encoder.getInputLength(size);
				if (inLength == 0)
					inLength = DEFAULT_INPUT_LENGTH;
				byte[] buffer = new byte[inLength];

				// Read and encode successive blocks of chunk data, appending each block to the XML element as a text
				// node, and merging each appended text node with previous character data
				long remainingSize = size;
				while (remainingSize > 0)
				{
					int length = (int)Math.min(remainingSize, buffer.length);
					reader.getDataInput().readFully(buffer, 0, length);
					try
					{
						String text = encoder.encode(buffer, 0, length, remainingSize == length);
						element.appendChild(xmlDocument.createTextNode(text));
					}
					catch (IllegalArgumentException e)
					{
						throw new NlfException(ExceptionId.ERROR_ENCODING_CHUNK_DATA);
					}
					element.normalize();
					remainingSize -= length;
				}
			}
			catch (OutOfMemoryError e)
			{
				throw new NlfException(ExceptionId.NOT_ENOUGH_MEMORY);
			}
		}
		return element;
	}

	//------------------------------------------------------------------

	/**
	 * Sets the parent of this chunk to the specified list.
	 *
	 * @param list
	 *          the list that will be set as the parent of this chunk.
	 * @since 1.0
	 */

	protected void setParent(
		ChunkList	list)
	{
		parent = list;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member interfaces
////////////////////////////////////////////////////////////////////////


	// INTERFACE: CHUNK-DATA READER


	/**
	 * This interface defines the methods that must be implemented by a class that reads chunk data from a data input.
	 * The data input is likely to be an instance of {@link RandomAccessFile}.
	 *
	 * @since 1.0
	 * @see   #getReader()
	 * @see   #setReader(Chunk.IReader)
	 */

	public interface IReader
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Resets the chunk reader before any reading is performed.
		 *
		 * @throws IOException
		 *           if an I/O error occurs.
		 * @since  1.0
		 */

		void reset()
			throws IOException;

		//--------------------------------------------------------------

		/**
		 * Returns the data input from which the chunk data will be read.  The input is likely to be an instance of
		 * {@link RandomAccessFile}.
		 *
		 * @return the data input from which the chunk data will be read.
		 * @since  1.0
		 */

		DataInput getDataInput();

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: CHUNK-DATA WRITER


	/**
	 * This interface defines the methods that must be implemented by a class that writes chunk data to a data output.
	 * The data output is likely to be an instance of {@link RandomAccessFile}.
	 * <p>
	 * The recommended way of writing a Nested-List File is with the {@link Document#write(File)} method, which writes a
	 * document in two passes.  It uses the value that is returned by the {@link #reset(int)} method on the first pass
	 * to determine whether a chunk should be rewritten on the second pass.
	 * </p>
	 *
	 * @since 1.0
	 * @see   #getWriter()
	 * @see   #setWriter(Chunk.IWriter)
	 */

	public interface IWriter
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Resets this chunk writer before any writing is performed.  The writer is reset at the start of each pass of
		 * the document writer.
		 *
		 * @param  pass
		 *           the index of the pass (0 = first pass, 1 = second pass) of the document writer that calls this
		 *           method.
		 * @return {@code true} on the first pass if the chunk should be rewritten on the second pass; {@code false}
		 *         otherwise.
		 * @throws IOException
		 *           if an I/O error occurs.
		 * @since  1.0
		 */

		boolean reset(
			int	pass)
			throws IOException;

		//--------------------------------------------------------------

		/**
		 * Returns the length (size) of the chunk in bytes.
		 *
		 * @return the length (size) of the chunk in bytes.
		 * @since  1.0
		 */

		long getLength();

		//--------------------------------------------------------------

		/**
		 * Writes the chunk data to the specified data output.  The data output is likely to be an instance of {@link
		 * RandomAccessFile}.
		 *
		 * @param  dataOutput
		 *           the data output to which the chunk data will be written.
		 * @throws IOException
		 *           if an I/O error occurs.
		 * @since  1.0
		 */

		void write(
			DataOutput	dataOutput)
			throws IOException;

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: CHUNK-DATA ENCODER


	/**
	 * This interface defines the methods that must be implemented by a class that encodes chunk data as character data
	 * for use as a text node of an XML file.  The encoder is used by the chunk's {@link #toXml(org.w3c.dom.Document)}
	 * method.
	 *
	 * @since 1.0
	 * @see   #getEncoder()
	 * @see   #setEncoder(Chunk.IEncoder)
	 */

	public interface IEncoder
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Resets this chunk encoder before any encoding is performed.
		 *
		 * @since 1.0
		 */

		void reset();

		//--------------------------------------------------------------

		/**
		 * Returns the length of a block of input data that the {@link #encode(byte[], int, int, boolean)} method
		 * expects to receive.  A value of zero indicates that the {@code encode} method will accept a block of any
		 * length.
		 *
		 * @param  size
		 *           the size of the chunk data.
		 * @return the length of a block of input data that the {@link #encode(byte[], int, int, boolean) encode} method
		 *         expects to receive; {@code 0}, if the {@code encode} method will accept a block of any length.
		 * @since  1.0
		 */

		int getInputLength(
			long	size);

		//--------------------------------------------------------------

		/**
		 * Encodes the specified byte data as a string.  When this encoder is used by the chunk's {@link
		 * #toXml(org.w3c.dom.Document)} method, this method will be called on successive blocks of the chunk data.
		 *
		 * @param  data
		 *           the data that will be encoded.
		 * @param  offset
		 *           the offset to {@code data} at which the input data begin.
		 * @param  length
		 *           the length of the input data.
		 * @param  endOfInput
		 *           {@code true} if {@code data} is the last block to be encoded; {@code false} otherwise.
		 * @return the string that results from encoding {@code data}.
		 * @throws IllegalArgumentException
		 *           if
		 *           <ul>
		 *             <li>any of the arguments are invalid, or</li>
		 *             <li>the input data is malformed or otherwise illegal.</li>
		 *           </ul>
		 * @since  1.0
		 */

		String encode(
			byte[]	data,
			int		offset,
			int		length,
			boolean	endOfInput);

		//--------------------------------------------------------------

	}

	//==================================================================


	// INTERFACE: CHUNK PROCESSOR


	/**
	 * This functional interface defines the method that must be implemented by a class that processes the chunks that
	 * are visited in the traversal of a document tree or subtree.
	 *
	 * @since 1.0
	 * @see   ChunkList#processChunks(NlfConstants.TraversalOrder, Chunk.IProcessor)
	 * @see   Document#processChunks(NlfConstants.TraversalOrder, Chunk.IProcessor)
	 */

	@FunctionalInterface
	public interface IProcessor
	{

	////////////////////////////////////////////////////////////////////
	//  Methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Processes the specified chunk.  During the traversal of a document tree or subtree, this method is called on
		 * every chunk that is visited.
		 * <p>
		 * A processor that wants to terminate the traversal of the tree should throw a {@link TerminatedException},
		 * which can wrap another {@code Throwable}.
		 * </p>
		 *
		 * @param  chunk
		 *           the chunk that will be processed.
		 * @throws TerminatedException
		 *           if the traversal of the tree or subtree was terminated.
		 * @since  1.0
		 * @see    TerminatedException
		 */

		void process(
			Chunk	chunk);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: BASE64 ENCODER


	/**
	 * This singleton class implements a Base64 encoder for chunk data.  It is set as the default encoder on all general
	 * chunks (ie, chunks that do not have a reserved identifier).  The length of a line of output from the encoder and
	 * the string that terminates each line can both be set on the single instance of the class.  The default line
	 * length of zero means that no line separators will be appended to the encoded output.
	 * <p>
	 * A chunk's encoder is used by the {@link #toXml(org.w3c.dom.Document)} method to encode the chunk data as the
	 * character data of an XML element.
	 * </p>
	 *
	 * @since 1.0
	 * @see   IEncoder
	 */

	public static class Base64Encoder
		implements IEncoder
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		/** The single instance of this class. */
		public static final		Base64Encoder	INSTANCE	= new Base64Encoder();

		private static final	int		MIN_LINE_LENGTH	= 0;
		private static final	int		MAX_LINE_LENGTH	= Integer.MAX_VALUE;

		private static final	String	LINE_SEPARATOR_PROPERTY_KEY	= "line.separator";

		private static final	String	DEFAULT_LINE_SEPARATOR	= "\n";

		private static final	String	BASE64_CHARS	=
				"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	int		lineLength;
		private	String	lineSeparator;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of a Base64 encoder.  This is a singleton class; its only instance is {@link
		 * #INSTANCE}.
		 *
		 * @since 1.0
		 */

		private Base64Encoder()
		{
			setLineSeparator(null);
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Encodes three bytes as four Base64 characters.
		 *
		 * @param  data
		 *           the three bytes that are to be encoded.
		 * @return an array of four characters that is the result of encoding the three input bytes as Base64.
		 * @since  1.0
		 */

		private static char[] getChars(
			int	data)
		{
			char[] chars = new char[4];
			for (int j = 3; j >= 0; j--)
			{
				chars[j] = BASE64_CHARS.charAt(data & 0x3F);
				data >>>= 6;
			}
			return chars;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IEncoder interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Resets this encoder before any encoding is performed: does nothing.
		 *
		 * @since 1.0
		 */

		@Override
		public void reset()
		{
			// do nothing
		}

		//--------------------------------------------------------------

		/**
		 * Returns the length of a block of input data that the {@link #encode(byte[], int, int, boolean)} method
		 * expects to receive.
		 *
		 * @param  size
		 *           the size of the chunk data.
		 * @return the length of a block of input data that the {@code encode(byte[], int, int, boolean)} method expects
		 *         to receive.
		 * @since  1.0
		 */

		@Override
		public int getInputLength(
			long	size)
		{
			return lineLength / 4 * 3;
		}

		//--------------------------------------------------------------

		/**
		 * Encodes the specified byte data as a Base64 string.  The line length and line separator that are used in the
		 * encoding can be set with {@link #setLineLength(int)} and {@link #setLineSeparator(String)} respectively.
		 *
		 * @param  data
		 *           the data that will be encoded.
		 * @param  offset
		 *           the offset to {@code data} at which the input data begin.
		 * @param  length
		 *           the length of the input data.
		 * @param  endOfInput
		 *           {@code true} if {@code data} is the last block to be encoded; {@code false} otherwise.
		 * @return the string that results from encoding {@code data}.
		 * @throws IllegalArgumentException
		 *           if
		 *           <ul>
		 *             <li>{@code data} is {@code null}, or</li>
		 *             <li>{@code (offset < 0)} or {@code (offset > data.length)}, or</li>
		 *             <li>{@code (length < 0)} or {@code (length > data.length - offset)}.</li>
		 *           </ul>
		 * @since  1.0
		 */

		@Override
		public String encode(
			byte[]	data,
			int		offset,
			int		length,
			boolean	endOfInput)
		{
			// Validate arguments
			if (data == null)
				throw new IllegalArgumentException("Null data");
			if ((offset < 0) || (offset > data.length))
				throw new IllegalArgumentException("Offset out of bounds");
			if ((length < 0) || (length > data.length - offset))
				throw new IllegalArgumentException("Length out of bounds");

			// Encode input data as Base64
			StringBuilder outBuffer = new StringBuilder(4 * length / 3);
			int inBuffer = 0;
			int inBufferLength = 0;
			int numChars = 0;
			int endOffset = offset + length;
			while (offset < endOffset)
			{
				inBuffer <<= 8;
				inBuffer |= data[offset++] & 0xFF;
				if (++inBufferLength >= 3)
				{
					numChars = appendChars(outBuffer, getChars(inBuffer), numChars);
					inBuffer = 0;
					inBufferLength = 0;
				}
			}
			if (inBufferLength > 0)
			{
				inBuffer <<= (3 - inBufferLength) << 3;
				char[] chars = getChars(inBuffer);
				for (int i = inBufferLength; i < chars.length; i++)
					chars[i] = '=';
				appendChars(outBuffer, chars, numChars);
			}
			if (lineLength > 0)
				outBuffer.append(lineSeparator);
			return outBuffer.toString();
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		/**
		 * Returns the length of a line of encoded output.
		 *
		 * @return the length of a line of encoded output.
		 * @since  1.0
		 * @see    #setLineLength(int)
		 */

		public int getLineLength()
		{
			return lineLength;
		}

		//--------------------------------------------------------------

		/**
		 * Returns the string that is appended to each line of encoded output.
		 *
		 * @return the string that is appended to each line of encoded output.
		 * @since  1.0
		 * @see    #setLineSeparator(String)
		 */

		public String getLineSeparator()
		{
			return lineSeparator;
		}

		//--------------------------------------------------------------

		/**
		 * Sets the length of a Base64-encoded line.  If the length is set to zero, no line separators are appended to
		 * the encoded output.  The length must be a multiple of {@code 4}.
		 *
		 * @param  lineLength
		 *           the length of a Base64-encoded line, or {@code 0} if no line separators will be appended to the
		 *           encoded output.  The length must be a multiple of {@code 4}.
		 * @throws IllegalArgumentException
		 *           if {@code lineLength} is negative or is not a multiple of {@code 4}.
		 * @since  1.0
		 * @see    #getLineLength()
		 */

		public void setLineLength(
			int	lineLength)
		{
			if ((lineLength < MIN_LINE_LENGTH) || (lineLength > MAX_LINE_LENGTH) || (lineLength % 4 != 0))
				throw new IllegalArgumentException();
			this.lineLength = lineLength;
		}

		//--------------------------------------------------------------

		/**
		 * Sets the string that is appended to each line of encoded output.
		 *
		 * @param lineSeparator
		 *          the string that will be appended to each line of encoded output.  If {@code lineSeparator} is {@code
		 *          null}, the line separator is set to the value of the system property {@code line.separator}.
		 * @since 1.0
		 * @see   #getLineSeparator()
		 */

		public void setLineSeparator(
			String	lineSeparator)
		{
			this.lineSeparator = (lineSeparator == null)
											? System.getProperty(LINE_SEPARATOR_PROPERTY_KEY, DEFAULT_LINE_SEPARATOR)
											: lineSeparator;
		}

		//--------------------------------------------------------------

		/**
		 * Appends characters to the specified buffer.
		 *
		 * @param  buffer
		 *           the buffer to which the characters will be appended.
		 * @param  chars
		 *           the characters that will be appended.
		 * @param  numChars
		 *           the number of encoded characters in the buffer.
		 * @return the updated number of encoded characters in the buffer after {@code chars} have been appended.
		 * @since  1.0
		 */

		private int appendChars(
			StringBuilder	buffer,
			char[]			chars,
			int				numChars)
		{
			if (lineLength == 0)
			{
				buffer.append(chars);
				numChars += chars.length;
			}
			else
			{
				for (char ch : chars)
				{
					if ((numChars > 0) && (numChars % lineLength == 0))
						buffer.append(lineSeparator);
					buffer.append(ch);
					++numChars;
				}
			}
			return numChars;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
