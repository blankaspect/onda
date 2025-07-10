/*====================================================================*\

Document.java

Class: Nested-List File document.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.OverlappingFileLockException;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import uk.blankaspect.common.filesystem.FilenameUtils;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE DOCUMENT


/**
 * This class implements a Nested-List File document.  A document is a representation of an entire Nested-List File.
 * The structure of the document is a rooted tree whose nodes are {@linkplain Chunk chunks}.  Leaf nodes are <i>simple
 * chunks</i>, branch nodes are {@linkplain ChunkList lists}, and the root node is the document's <i>root list</i>.
 * <p>
 * This class provides methods for reading from and writing to a Nested-List File, which, because of the need to seek
 * backwards and forwards in the file, is opened as a {@linkplain RandomAccessFile random-access file}, rather than as
 * an input stream or output stream.  The methods of this class supervise the reading and writing of a document, but the
 * content of simple chunks is read and written by the {@linkplain Chunk.IReader reader} and {@linkplain Chunk.IWriter
 * writer} that are set on each chunk.
 * </p>
 * <p>
 * Reading a document is done in two passes: on the first pass, the file is read and parsed to determine the document's
 * structure (ie, to generate its tree); on the second pass, the content of the simple chunks is read.  The first pass
 * is performed by the {@link #read(File)} method of this class, which sets the identifier and size of each chunk, and
 * sets a default {@link Document.ChunkReader ChunkReader} object on each chunk to allow its data to be read from the
 * file.  The second pass is performed by the code that wants to access the chunk data, by calling the methods that are
 * provided by the chunk's {@linkplain Chunk.IReader reader}.  Because the document's tree and the location of each
 * chunk are known after the first pass, the chunks can be processed in any order during the second pass, and the chunk
 * data can be read multiple times.
 * </p>
 * <p>
 * Writing a document is not as straightforward as reading it because the end of a chunk is not marked by a delimiter
 * but is specified implicitly by the <i>size</i> field in the chunk header.  If the size of a chunk is not known when
 * its chunk header is written, the header must be written with a dummy size, then rewritten after the chunk data have
 * been written and their size determined.  The mechanism used by this class's {@link #write(File)} method is for a
 * chunk to indicate, by returning a negative value from its {@link Chunk#getSize() getSize()} method, that its size is
 * unknown when its header is written.  In this case, the <i>size</i> field in the header is fixed up after the chunk
 * data are written.
 * </p>
 * <p>
 * To facilitate the writing of chunks whose content is not fully known when it is written (eg, because it depends on
 * subsequent chunks), the {@link #write(File)} method writes chunk data in two passes.  On the first pass, a chunk can
 * signal, through the value returned by the {@link Chunk.IWriter#reset(int) reset(int)} method of its writer, that it
 * should be written again on the second pass.  The size of the chunk must not change between the first pass and the
 * second pass.  An {@linkplain Attributes attributes chunk} uses its {@link Attributes#setRewrite(boolean)
 * setRewrite(boolean)} method to set the flag that is used by its default writer.
 * </p>
 * <p>
 * After a document has been read or written, the random-access file that was opened on the document should be closed by
 * calling one of the document's <i>close</i> methods: {@link #close()} or {@link #closeIgnoreException()}.  The latter,
 * as its name suggests, ignores any exception that is thrown when the file is closed.
 * </p>
 *
 * @since 1.0
 * @see   Chunk
 * @see   ChunkList
 */

public class Document
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The identifier of a Nested-List File. */
	public static final		byte[]	FILE_ID	= { (byte)0x95, 'N', 'L', 'F' };

	// Current version, minumum and maximum supported versions
	private static final	int		VERSION					= 0;
	private static final	int		MIN_SUPPORTED_VERSION	= 0;
	private static final	int		MAX_SUPPORTED_VERSION	= 0;

	// Sizes of components of file header
	private static final	int		FILE_ID_SIZE	= FILE_ID.length;
	private static final	int		VERSION_SIZE	= 2;
	private static final	int		FLAGS_SIZE		= 1;
	private static final	int		RESERVED_SIZE	= 1;
	private static final	int		HEADER_SIZE		= FILE_ID_SIZE + VERSION_SIZE + FLAGS_SIZE + RESERVED_SIZE;

	// Masks for flags in file header
	private static final	int		BYTE_ORDER_MASK	= 1 << 0;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	RandomAccessFile	raFile;
	private	ChunkList			rootList;
	private	boolean				littleEndian;
	private	List<ChunkOffset>	rewrites;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a document with the specified byte order.
	 *
	 * @param littleEndian
	 *          if {@code true}, the byte order of <i>size</i> fields in the document will be little-endian; if {@code
	 *          false}, the byte order will be big-endian.
	 * @since 1.0
	 */

	public Document(
		boolean	littleEndian)
	{
		// Initialise instance variables
		this.littleEndian = littleEndian;
		rewrites = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the root list of this document.
	 *
	 * @return the root list of this document.
	 * @since  1.0
	 */

	public ChunkList getRootList()
	{
		return rootList;
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if the byte order of <i>size</i> fields in the document is little-endian; {@code false} if
	 * the byte order is big-endian.
	 *
	 * @return {@code true} if the byte order of <i>size</i> fields in the document is little-endian; {@code false} if
	 *         the byte order is big-endian.
	 * @since  1.0
	 */

	public boolean isLittleEndian()
	{
		return littleEndian;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a general {@linkplain Chunk chunk} with the specified identifier.  The chunk will belong to this document
	 * and mau be added only to a list that belongs to this document.
	 *
	 * @param  id
	 *           the identifier of the chunk.
	 * @return a general chunk with identifier {@code id} that belongs to this document.
	 * @throws NlfUncheckedException
	 *           RESERVED_IDENTIFIER: if {@code id} is a reserved identifier.
	 * @since  1.0
	 */

	public Chunk createChunk(
		Id	id)
		throws NlfUncheckedException
	{
		// Test for reserved ID
		if (id.isReserved())
			throw new NlfUncheckedException(ExceptionId.RESERVED_IDENTIFIER);

		// Create new chunk and return it
		return new Chunk(this, id);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a {@linkplain ChunkList chunk list} with the specified list-instance identifier.  The list will belong to
	 * this document: it may contain only chunks and lists that belong to this document, and it may be added only to
	 * another list that belongs to this document.
	 *
	 * @param  instanceId
	 *           the list-instance identifier of the list.
	 * @return a list with list-instance identifier {@code instanceId} that belongs to this document.
	 * @throws NlfUncheckedException
	 *           RESERVED_IDENTIFIER: if {@code instanceId} is a reserved identifier.
	 * @since  1.0
	 */

	public ChunkList createList(
		Id	instanceId)
		throws NlfUncheckedException
	{
		// Test for reserved ID
		if (instanceId.isReserved())
			throw new NlfUncheckedException(ExceptionId.RESERVED_IDENTIFIER);

		// Create new chunk list and return it
		return new ChunkList(this, instanceId);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a root list for this document with the specified list-instance identifier.  The root list of this
	 * document will be set to the list that is created by this method.  The list will be belong to this document and
	 * may contain only chunks and lists that belong to this document.
	 *
	 * @param  instanceId
	 *           the list-instance identifier of the root list.
	 * @return a list with list-instance identifier {@code instanceId} that belongs to this document and that has
	 *         been set as the root list of this document.
	 * @throws NlfUncheckedException
	 *           RESERVED_IDENTIFIER: if {@code instanceId} is a reserved identifier.
	 * @since  1.0
	 */

	public ChunkList createRootList(
		Id	instanceId)
		throws NlfUncheckedException
	{
		rootList = createList(instanceId);
		return rootList;
	}

	//------------------------------------------------------------------

	/**
	 * Reads the specified file and parses it to create a list tree for this document.  The file is opened as a {@link
	 * RandomAccessFile} for reading only, and the file remains open with a shared lock when this method returns
	 * normally.  (The file is closed if the method terminates with an exception.)  The random-access file should be
	 * closed with {@link #close()} when the chunk data have been read and no further file access is required.
	 * <p>
	 * This method sets the identifier and size of each chunk, and sets a default {@linkplain Chunk.IReader reader} on
	 * each chunk to allow its data to be read from the file.
	 * </p>
	 *
	 * @param  file
	 *           the file that will be read.
	 * @throws NlfException
	 *           if
	 *           <ul>
	 *             <li>the specified file cannot be opened, accessed or locked, or</li>
	 *             <li>an error occurs when reading the file, or</li>
	 *             <li>the file is malformed or otherwise invalid.</li>
	 *           </ul>
	 * @throws NlfUncheckedException
	 *           FILE_IS_OPEN_ON_DOCUMENT: if a random-access file is already open on this document.
	 * @since  1.0
	 */

	public void read(
		File	file)
		throws NlfException, NlfUncheckedException
	{
		// Test whether a file is already open
		if (raFile != null)
			throw new NlfUncheckedException(ExceptionId.FILE_IS_OPEN_ON_DOCUMENT);

		// Open, read and parse file
		try
		{
			// Open file for reading only
			try
			{
				raFile = new RandomAccessFile(file, "r");
			}
			catch (FileNotFoundException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_OPEN_FILE, e);
			}
			catch (SecurityException e)
			{
				throw new NlfException(ExceptionId.FILE_ACCESS_NOT_PERMITTED, e);
			}

			// Lock file
			try
			{
				if (raFile.getChannel().tryLock(0, Long.MAX_VALUE, true) == null)
					throw new NlfException(ExceptionId.FAILED_TO_LOCK_FILE, file);
			}
			catch (OverlappingFileLockException e)
			{
				// ignore
			}
			catch (IOException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_LOCK_FILE, file, e);
			}

			// Read and parse file
			try
			{
				parse(file);
			}
			catch (IOException e)
			{
				throw new NlfException(ExceptionId.ERROR_READING_FILE, file, e);
			}
		}
		catch (NlfException e)
		{
			// Close file
			closeIgnoreException();

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Writes this document to the specified file.  This method calls the {@linkplain Chunk.IWriter writer} of each
	 * chunk to write the chunk's data.
	 * <p>
	 * If the size of a chunk is not known when its chunk header is written, the header will be written with a dummy
	 * size, then rewritten after the chunk data have been written and their size determined.  A chunk can indicate, by
	 * returning a negative value from its {@link Chunk#getSize() getSize()} method, that its size is unknown when its
	 * header is written.  In this case, the <i>size</i> field in the header is fixed up after the chunk data is
	 * written.
	 * </p>
	 * <p>
	 * To facilitate the writing of chunks whose content depends on subsequent chunks, this method writes chunk data in
	 * two passes.  On the first pass, a chunk can signal, through the value returned by the {@link
	 * Chunk.IWriter#reset(int) reset(int)} method of its writer, that it should be written again on the second pass.
	 * The size of the chunk data must not change between the first pass and the second pass.
	 * </p>
	 * <p>
	 * The document is written to a temporary file, which is renamed to the specified file when the document has been
	 * successfully written.  Until the temporary file has been written, an exception that is thrown by this method will
	 * refer to the temporary file, not to the specified file.  The temporary file will be deleted if a handled
	 * exception is thrown, unless an existing file with the same pathname as the the specified file has been deleted
	 * prior to renaming the temporary file.
	 * </p>
	 *
	 * @param  file
	 *           the file that will be written.
	 * @throws NlfException
	 *           if
	 *           <ul>
	 *             <li>a temporary file cannot be opened, accessed, locked or closed, or</li>
	 *             <li>an error occurs when writing the file, or</li>
	 *             <li>an existing file with the same pathname as the specified file cannot be deleted, or</li>
	 *             <li>the temporary file cannot be renamed to the specified file after it is written.</li>
	 *           </ul>
	 * @throws NlfUncheckedException
	 *           FILE_IS_OPEN_ON_DOCUMENT: if a random-access file is already open on this document.
	 * @since  1.0
	 */

	public void write(
		File	file)
		throws NlfException, NlfUncheckedException
	{
		// Test whether a file is already open
		if (raFile != null)
			throw new NlfUncheckedException(ExceptionId.FILE_IS_OPEN_ON_DOCUMENT);

		// Write file
		File tempFile = null;
		boolean oldFileDeleted = false;
		try
		{
			// Create parent directory of output file
			File directory = file.getAbsoluteFile().getParentFile();
			if ((directory != null) && !directory.exists())
			{
				try
				{
					if (!directory.mkdirs())
						throw new NlfException(ExceptionId.FAILED_TO_CREATE_DIRECTORY, directory);
				}
				catch (SecurityException e)
				{
					throw new NlfException(ExceptionId.FAILED_TO_CREATE_DIRECTORY, directory, e);
				}
			}

			// Create temporary file
			try
			{
				tempFile = FilenameUtils.tempLocation(file);
				tempFile.createNewFile();
			}
			catch (Exception e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_CREATE_TEMPORARY_FILE, e);
			}

			// Open temporary file for reading and writing
			try
			{
				raFile = new RandomAccessFile(tempFile, "rw");
			}
			catch (FileNotFoundException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_OPEN_FILE, tempFile, e);
			}
			catch (SecurityException e)
			{
				throw new NlfException(ExceptionId.FILE_ACCESS_NOT_PERMITTED, tempFile, e);
			}

			// Lock file
			try
			{
				if (raFile.getChannel().tryLock() == null)
					throw new NlfException(ExceptionId.FAILED_TO_LOCK_FILE, tempFile);
			}
			catch (IOException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_LOCK_FILE, tempFile, e);
			}

			// Write file
			try
			{
				// Write file header
				writeHeader();

				// Write root list
				rewrites.clear();
				if (rootList != null)
					writeList(rootList);

				// Rewrite chunks
				for (ChunkOffset rewrite : rewrites)
					rewriteChunk(rewrite.chunk, rewrite.offset);
			}
			catch (IOException e)
			{
				throw new NlfException(ExceptionId.ERROR_WRITING_FILE, file, e);
			}

			// Close file
			try
			{
				raFile.close();
				raFile = null;
			}
			catch (IOException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_CLOSE_FILE, file, e);
			}

			// Delete any existing file
			try
			{
				if (file.exists() && !file.delete())
					throw new NlfException(ExceptionId.FAILED_TO_DELETE_FILE, file);
				oldFileDeleted = true;
			}
			catch (SecurityException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_DELETE_FILE, file, e);
			}

			// Rename temporary file
			try
			{
				if (!tempFile.renameTo(file))
					throw new NlfException(ExceptionId.FAILED_TO_RENAME_FILE, tempFile);
			}
			catch (SecurityException e)
			{
				throw new NlfException(ExceptionId.FAILED_TO_RENAME_FILE, tempFile, e);
			}
		}
		catch (NlfException e)
		{
			// Close file
			closeIgnoreException();

			// Delete temporary file
			try
			{
				if (!oldFileDeleted && (tempFile != null) && tempFile.exists())
					tempFile.delete();
			}
			catch (Exception e1)
			{
				// ignore
			}

			// Rethrow exception
			throw e;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Closes a random-access file that is open on this document.
	 *
	 * @throws IOException
	 *           if an error occurs when closing the file.
	 * @since  1.0
	 * @see    #closeIgnoreException()
	 */

	public void close()
		throws IOException
	{
		if (raFile != null)
		{
			RandomAccessFile tempRaFile = raFile;
			raFile = null;
			tempRaFile.close();
		}
	}

	//------------------------------------------------------------------

	/**
	 * Closes a random-access file that is open on this document, ignoring any exception that occurs when the file is
	 * closed.  This method is intended to be used by an exception handler.
	 *
	 * @since 1.0
	 * @see   #close()
	 */

	public void closeIgnoreException()
	{
		if (raFile != null)
		{
			try
			{
				raFile.close();
			}
			catch (IOException e)
			{
				// ignore
			}
			raFile = null;
		}
	}

	//------------------------------------------------------------------

	/**
	 * Generates an XML document from this document and returns the result.
	 *
	 * @return the XML document that is generated from this document.
	 * @throws NlfException
	 *           <ul>
	 *             <li>FAILED_TO_CREATE_XML_DOCUMENT: if an exception occurs in creating the XML document;</li>
	 *             <li>ERROR_GENERATING_XML_DOCUMENT: if an exception occurs in generating one of the elements of the
	 *                 XML document.</li>
	 *           </ul>
	 * @since  1.0
	 */

	public org.w3c.dom.Document toXml()
		throws NlfException
	{
		// Create XML document
		org.w3c.dom.Document xmlDocument = null;
		try
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newNSInstance();
			docBuilderFactory.setValidating(false);
			DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
			xmlDocument = documentBuilder.getDOMImplementation()
					.createDocument(rootList.getLocalNamespaceName(), rootList.getInstanceId().getValue(), null);
		}
		catch (Throwable e)
		{
			throw new NlfException(ExceptionId.FAILED_TO_CREATE_XML_DOCUMENT, e);
		}

		// Generate XML document from this document
		try
		{
			if (rootList != null)
				rootList.toXml(xmlDocument, xmlDocument.getDocumentElement());
		}
		catch (NlfException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new NlfException(ExceptionId.ERROR_GENERATING_XML_DOCUMENT, e);
		}

		return xmlDocument;
	}

	//------------------------------------------------------------------

	/**
	 * Traverses the document tree in the specified order, calling the specified processor on each chunk that is
	 * visited.
	 *
	 * @param  traversalOrder
	 *           the order, breadth-first or depth-first, in which the tree will be traversed.
	 * @param  processor
	 *           the processor that will be called on each chunk that is visited.
	 * @throws IllegalArgumentException
	 *           if {@code traversalOrder} is {@code null} or {@code processor} is {@code null}.
	 * @throws TerminatedException
	 *           if the traversal of the tree was terminated.
	 * @since  1.0
	 */

	public void processChunks(
		NlfConstants.TraversalOrder	traversalOrder,
		Chunk.IProcessor			processor)
	{
		// Validate arguments
		if ((traversalOrder == null) || (processor == null))
			throw new IllegalArgumentException();

		// Process chunks of root list
		if (rootList != null)
			rootList.processChunks(traversalOrder, processor);
	}

	//------------------------------------------------------------------

	/**
	 * Parses a Nested-List File document, constructing the list tree for the document.
	 *
	 * @param  file
	 *           the file that will be parsed.
	 * @throws IOException
	 *           if an error occurs when parsing the document.
	 * @throws NlfException
	 *           if the file is malformed or otherwise invalid.
	 * @since  1.0
	 */

	private void parse(
		File	file)
		throws IOException, NlfException
	{
		// Test for file header
		if (raFile.length() < HEADER_SIZE)
			throw new NlfException(ExceptionId.NOT_A_NESTED_LIST_FILE, file);

		// Read and validate file identifier
		byte[] buffer = new byte[FILE_ID_SIZE];
		raFile.readFully(buffer);
		if (!Arrays.equals(buffer, FILE_ID))
			throw new NlfException(ExceptionId.NOT_A_NESTED_LIST_FILE, file);

		// Read and validate file version
		buffer = new byte[VERSION_SIZE];
		raFile.readFully(buffer);
		try
		{
			int version = Integer.parseInt(new String(buffer, StandardCharsets.US_ASCII));
			if ((version < MIN_SUPPORTED_VERSION) || (version > MAX_SUPPORTED_VERSION))
				throw new NlfException(ExceptionId.UNSUPPORTED_VERSION, file);
		}
		catch (NumberFormatException e)
		{
			throw new NlfException(ExceptionId.INVALID_VERSION_NUM, file);
		}

		// Read flags
		int flags = raFile.readByte() & 0xFF;
		littleEndian = ((flags & BYTE_ORDER_MASK) != 0);

		// Skip reserved bytes
		raFile.skipBytes(RESERVED_SIZE);

		// Read root list
		Chunk chunk = parseChunk(file, null);
		if (!chunk.isList())
			throw new NlfException(ExceptionId.NO_ROOT_LIST, file);
		rootList = (ChunkList)chunk;
	}

	//------------------------------------------------------------------

	/**
	 * Parses a chunk at the current offset in the specified file.  If the chunk is a list, a {@link ChunkList} is
	 * created, initialised and added to the specified parent list with {@link #parseList(File, ChunkList, long)};
	 * otherwise, a {@link Chunk} is created, initialised and added to the specified parent list.  This method returns
	 * the chunk or list that was created.
	 *
	 * @param  file
	 *           the file that will be parsed.
	 * @param  parent
	 *           the list that will be the parent of the chunk or list that is created.
	 * @return the chunk or list that was created.
	 * @throws IOException
	 *           if an error occurs when parsing the chunk.
	 * @throws NlfException
	 *           if the file is malformed or otherwise invalid.
	 * @since  1.0
	 * @see    #parseAttributes(File, ChunkList, long)
	 * @see    #parseList(File, ChunkList, long)
	 */

	private Chunk parseChunk(
		File		file,
		ChunkList	parent)
		throws IOException, NlfException
	{
		// Get file offset and length
		long offset = raFile.getFilePointer();
		long fileLength = raFile.length();

		// Read size of chunk ID
		if (fileLength - offset < Id.SIZE_SIZE)
			throw new NlfException(ExceptionId.MALFORMED_FILE, file, offset);
		int idSize = raFile.readByte() & Id.SIZE_MASK;
		if ((idSize < Id.MIN_SIZE) || (idSize > Id.MAX_SIZE) ||
			 (idSize > fileLength - (offset + Id.SIZE_SIZE)))
			throw new NlfException(ExceptionId.INVALID_CHUNK_ID, file, offset);

		// Read and validate chunk ID
		byte[] buffer = new byte[idSize];
		raFile.readFully(buffer);
		Id id = null;
		try
		{
			id = new Id(NlfUtils.utf8ToString(buffer));
		}
		catch (IllegalArgumentException e)
		{
			throw new NlfException(ExceptionId.INVALID_CHUNK_ID, file, offset);
		}
		offset += Id.SIZE_SIZE + idSize;

		// Read chunk size
		if (fileLength - offset < Chunk.SIZE_SIZE)
			throw new NlfException(ExceptionId.MALFORMED_FILE, file, offset);
		buffer = new byte[Chunk.SIZE_SIZE];
		raFile.readFully(buffer);
		long size = Utils.bytesToLong(buffer, 0, buffer.length, littleEndian);
		if (size < 0)
			throw new NlfException(ExceptionId.CHUNK_SIZE_OUT_OF_BOUNDS, file, offset);

		// Validate chunk size
		if (fileLength - (offset + Chunk.SIZE_SIZE) < size)
			throw new NlfException(ExceptionId.MALFORMED_FILE, file, offset);
		offset += Chunk.SIZE_SIZE;

		// If chunk is attributes chunk, parse it and return an attributes chunk
		if (id.equals(Attributes.ATTRIBUTES_ID))
			return parseAttributes(file, parent, size);

		// If chunk is list, parse it and return a list
		if (id.equals(ChunkList.LIST_ID))
			return parseList(file, parent, size);

		// Create chunk, initialise it, add it to parent list and return it
		Chunk chunk = new Chunk(this, id);
		chunk.setSize(size);
		chunk.setReader(new ChunkReader(offset));
		chunk.setParent(parent);
		parent.appendChunk(chunk);
		return chunk;
	}

	//------------------------------------------------------------------

	/**
	 * Parses an attributes chunk in the specified file.  This method is called by {@link #parseChunk(File, ChunkList)}
	 * when it finds that the chunk that it is parsing is an attributes chunk.  An {@link Attributes} object is created,
	 * initialised and added to the specified parent list.  This method returns the {@code Attributes} object that was
	 * created.
	 *
	 * @param  file
	 *           the file that will be parsed.
	 * @param  parent
	 *           the list that will be the parent of the attributes chunk that is created.
	 * @param  size
	 *           the size of the chunk.
	 * @return the attributes chunk that was created.
	 * @throws IOException
	 *           if an error occurs when parsing the chunk.
	 * @throws NlfException
	 *           if the file is malformed or otherwise invalid.
	 * @since  1.0
	 * @see    #parseChunk(File, ChunkList)
	 */

	private Attributes parseAttributes(
		File		file,
		ChunkList	parent,
		long		size)
		throws IOException, NlfException
	{
		// Get file offset and end offset
		long offset = raFile.getFilePointer();
		long endOffset = offset + size;

		// Create and initialise attributes chunk
		Attributes attributes = parent.createAttributes();
		if (attributes == null)
			throw new NlfException(ExceptionId.MULTIPLE_ATTRIBUTES_CHUNKS, file, offset);
		attributes.setReader(new ChunkReader(offset));

		// Parse attributes
		while (offset < endOffset)
		{
			// Read size of name
			if (endOffset - offset < Attributes.NAME_SIZE_SIZE)
				throw new NlfException(ExceptionId.MALFORMED_ATTRIBUTES_CHUNK, file, offset);
			byte[] buffer = new byte[Attributes.NAME_SIZE_SIZE];
			raFile.readFully(buffer);
			int nameSize = Utils.bytesToInt(buffer, 0, buffer.length, littleEndian) & Attributes.NAME_SIZE_MASK;
			if ((nameSize < Attributes.MIN_NAME_SIZE) || (nameSize > Attributes.MAX_NAME_SIZE) ||
				 (nameSize > endOffset - (offset + Attributes.NAME_SIZE_SIZE)))
				throw new NlfException(ExceptionId.INVALID_ATTRIBUTE_NAME, file, offset);

			// Read name
			String name = null;
			buffer = new byte[nameSize];
			raFile.readFully(buffer);
			try
			{
				name = NlfUtils.utf8ToString(buffer);
				if (!Attributes.isValidName(name))
					throw new IllegalArgumentException();
			}
			catch (IllegalArgumentException e)
			{
				throw new NlfException(ExceptionId.INVALID_ATTRIBUTE_NAME, file, offset);
			}

			// Test for attribute with the same name
			if (attributes.indexOf(name) >= 0)
				throw new NlfException(ExceptionId.MULTIPLE_ATTRIBUTES, file, offset);
			offset += Attributes.NAME_SIZE_SIZE + nameSize;

			// Read size of value
			if (endOffset - offset < Attributes.VALUE_SIZE_SIZE)
				throw new NlfException(ExceptionId.MALFORMED_ATTRIBUTES_CHUNK, file, offset);
			buffer = new byte[Attributes.VALUE_SIZE_SIZE];
			raFile.readFully(buffer);
			int valueSize = Utils.bytesToInt(buffer, 0, buffer.length, littleEndian) & Attributes.VALUE_SIZE_MASK;
			if ((valueSize < Attributes.MIN_VALUE_SIZE) || (valueSize > Attributes.MAX_VALUE_SIZE)
				|| (valueSize > endOffset - (offset + Attributes.VALUE_SIZE_SIZE)))
				throw new NlfException(ExceptionId.INVALID_ATTRIBUTE_VALUE, file, offset);

			// Read value
			String value = null;
			buffer = new byte[valueSize];
			raFile.readFully(buffer);
			try
			{
				value = NlfUtils.utf8ToString(buffer);
				if (!NlfUtils.isUtf8LengthWithinBounds(value, Attributes.MIN_VALUE_SIZE, Attributes.MAX_VALUE_SIZE))
					throw new IllegalArgumentException();
			}
			catch (IllegalArgumentException e)
			{
				throw new NlfException(ExceptionId.INVALID_ATTRIBUTE_VALUE, file, offset);
			}
			offset += Attributes.VALUE_SIZE_SIZE + valueSize;

			// Add attribute to end of list
			attributes.setAttribute(new Attributes.Attr(name, value));
		}

		// Return attributes
		return attributes;
	}

	//------------------------------------------------------------------

	/**
	 * Parses a chunk list in the specified file.  This method is called by {@link #parseChunk(File, ChunkList)} when it
	 * finds that the chunk that it is parsing is a list.  A {@link ChunkList} object is created, initialised and added
	 * to the specified parent list, then the list's child chunks are parsed recursively with {@link #parseChunk(File,
	 * ChunkList)}.  This method returns the {@code ChunkList} object that was created.
	 *
	 * @param  file
	 *           the file that will be parsed.
	 * @param  parent
	 *           the list that will be the parent of the list that is created.
	 * @param  size
	 *           the size of the list.
	 * @return the list that was created.
	 * @throws IOException
	 *           if an error occurs when parsing the chunk list.
	 * @throws NlfException
	 *           if the file is malformed or otherwise invalid.
	 * @since  1.0
	 * @see    #parseChunk(File, ChunkList)
	 */

	private ChunkList parseList(
		File		file,
		ChunkList	parent,
		long		size)
		throws IOException, NlfException
	{
		// Get file offset and end offset
		long offset = raFile.getFilePointer();
		long endOffset = offset + size;

		// Read size of list-instance identifier
		if (endOffset - offset < Id.SIZE_SIZE)
			throw new NlfException(ExceptionId.MALFORMED_FILE, file, offset);
		int idSize = raFile.readByte() & Id.SIZE_MASK;
		if ((idSize < Id.MIN_SIZE) || (idSize > Id.MAX_SIZE) || (idSize > endOffset - (offset + Id.SIZE_SIZE)))
			throw new NlfException(ExceptionId.INVALID_LIST_INSTANCE_ID, file, offset);

		// Read and validate list-instance identifier
		byte[] buffer = new byte[idSize];
		raFile.readFully(buffer);
		Id instanceId = null;
		try
		{
			instanceId = new Id(NlfUtils.utf8ToString(buffer));
		}
		catch (IllegalArgumentException e)
		{
			throw new NlfException(ExceptionId.INVALID_LIST_INSTANCE_ID, file, offset);
		}
		offset += Id.SIZE_SIZE + idSize;

		// Read size of namespace name
		if (endOffset - offset < ChunkList.NAMESPACE_NAME_SIZE_SIZE)
			throw new NlfException(ExceptionId.MALFORMED_FILE, file, offset);
		buffer = new byte[ChunkList.NAMESPACE_NAME_SIZE_SIZE];
		raFile.readFully(buffer);
		int nsNameSize = Utils.bytesToInt(buffer, 0, buffer.length, littleEndian) & ChunkList.NAMESPACE_NAME_SIZE_MASK;
		if ((nsNameSize < ChunkList.MIN_NAMESPACE_NAME_SIZE) || (nsNameSize > ChunkList.MAX_NAMESPACE_NAME_SIZE)
			|| (nsNameSize > endOffset - (offset + ChunkList.NAMESPACE_NAME_SIZE_SIZE)))
			throw new NlfException(ExceptionId.INVALID_NAMESPACE_NAME, file, offset);

		// Read namespace name
		String namespaceName = null;
		if (nsNameSize > 0)
		{
			buffer = new byte[nsNameSize];
			raFile.readFully(buffer);
			try
			{
				namespaceName = NlfUtils.utf8ToString(buffer);
			}
			catch (IllegalArgumentException e)
			{
				throw new NlfException(ExceptionId.INVALID_NAMESPACE_NAME, file, offset);
			}
		}

		// Create list
		ChunkList list = null;
		try
		{
			list = new ChunkList(this, instanceId, namespaceName);
		}
		catch (Exception e)
		{
			throw new NlfException(ExceptionId.INVALID_NAMESPACE_NAME, file, offset);
		}
		offset += ChunkList.NAMESPACE_NAME_SIZE_SIZE + nsNameSize;

		// Initialise list and add it to parent list
		list.setSize(size);
		if (parent != null)
		{
			list.setParent(parent);
			parent.appendChunk(list);
		}

		// Parse child chunks
		while (offset < endOffset)
		{
			Chunk chunk = parseChunk(file, list);
			offset += chunk.getHeaderSize() + chunk.getSize();
			raFile.seek(offset);
		}

		// Return list
		return list;
	}

	//------------------------------------------------------------------

	/**
	 * Writes the Nested-List File header to the random-access file that is open on this document.
	 *
	 * @throws IOException
	 *           if an error occurs when writing the header to the random-access file.
	 * @since  1.0
	 * @see    #writeChunk(Chunk)
	 */

	private void writeHeader()
		throws IOException
	{
		// Write NLF identifier
		raFile.write(FILE_ID);

		// Write NLF version number
		byte[] buffer = new byte[VERSION_SIZE];
		Arrays.fill(buffer, (byte)'0');
		int index = buffer.length;
		int value = VERSION;
		while (value > 0)
		{
			buffer[--index] += value % 10;
			value /= 10;
		}
		raFile.write(buffer);

		// Write flags
		int flags = 0;
		if (littleEndian)
			flags |= BYTE_ORDER_MASK;
		raFile.writeByte(flags);

		// Write reserved bytes
		raFile.write(new byte[RESERVED_SIZE]);
	}

	//------------------------------------------------------------------

	/**
	 * Writes the specified chunk to the random-access file that is open on this document.
	 *
	 * @param  chunk
	 *           the chunk that will be written.
	 * @throws IOException
	 *           if an error occurs when writing the chunk to the random-access file.
	 * @since  1.0
	 * @see    #rewriteChunk(Chunk, long)
	 * @see    #writeList(ChunkList)
	 */

	private void writeChunk(
		Chunk	chunk)
		throws IOException
	{
		// Write chunk header
		chunk.writeHeader(raFile);

		// Get offset to start of data
		long offset = raFile.getFilePointer();

		// Write chunk data
		Chunk.IWriter chunkWriter = chunk.getWriter();
		if (chunkWriter != null)
		{
			if (chunkWriter.reset(0))
				rewrites.add(new ChunkOffset(chunk, offset));
			chunkWriter.write(raFile);
		}

		// Fix up size of chunk
		if (chunk.getSize() < 0)
			fixChunkSize(chunk, offset);
	}

	//------------------------------------------------------------------

	/**
	 * Seeks to the specified offset in the random-access file that is open on this document, then writes the specified
	 * chunk to the file.
	 *
	 * @param  chunk
	 *           the chunk that will be written.
	 * @param  offset
	 *           the offset in the random-access file at which the chunk will be written.
	 * @throws IOException
	 *           if an error occurs when seeking or when writing the chunk to the random-access file.
	 * @since  1.0
	 * @see    #writeChunk(Chunk)
	 */

	private void rewriteChunk(
		Chunk	chunk,
		long	offset)
		throws IOException
	{
		// Seek start of chunk data
		raFile.seek(offset);

		// Write chunk data
		Chunk.IWriter chunkWriter = chunk.getWriter();
		chunkWriter.reset(1);
		chunkWriter.write(raFile);
	}

	//------------------------------------------------------------------

	/**
	 * Writes the specified chunk list to the random-access file that is open on this document.
	 *
	 * @param  list
	 *           the chunk list that will be written.
	 * @throws IOException
	 *           if an error occurs when writing the chunk list to the random-access file.
	 * @since  1.0
	 * @see    #writeChunk(Chunk)
	 */

	private void writeList(
		ChunkList	list)
		throws IOException
	{
		// Write chunk header of list
		list.writeHeader(raFile);

		// Get offset to start of data
		long offset = raFile.getFilePointer();

		// Write list header extension
		list.writeHeaderExtension(raFile);

		// Write child chunks
		boolean fixSize = false;
		for (int i = 0; i < list.getNumChunks(); i++)
		{
			Chunk chunk = list.getChunk(i);
			if (chunk.isList())
				writeList((ChunkList)chunk);
			else
			{
				if (chunk.getSize() < 0)
					fixSize = true;
				writeChunk(chunk);
			}
		}

		// Fix up size of list
		if (fixSize)
			fixChunkSize(list, offset);
	}

	//------------------------------------------------------------------

	/**
	 * Fixes up the size of the specified chunk after the chunk data have been written to the random-access file that is
	 * open on this document.  The size of the chunk is calculated from the current file position and the specified
	 * start offset.  The size of the chunk is set, then the size is written to the chunk header in the file.
	 *
	 * @param  chunk
	 *           the chunk whose size will be fixed up.
	 * @param  startOffset
	 *           the start offset of the chunk data in the file.
	 * @throws IOException
	 *           if an I/O error occurs.
	 * @since  1.0
	 * @see    #writeChunk(Chunk)
	 * @see    #writeList(ChunkList)
	 */

	private void fixChunkSize(
		Chunk	chunk,
		long	startOffset)
		throws IOException
	{
		long endOffset = raFile.getFilePointer();
		chunk.setSize(endOffset - startOffset);
		raFile.seek(startOffset - Chunk.SIZE_SIZE);
		raFile.write(chunk.getSizeBytes(littleEndian));
		raFile.seek(endOffset);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member records
////////////////////////////////////////////////////////////////////////


	// RECORD: PAIRING OF CHUNK AND FILE OFFSET


	/**
	 * This class implements a pairing of a chunk and a file offset.  It is used as the element type in the list of
	 * chunks that have requested to be rewritten on the second pass of the document writer.
	 *
	 * @param chunk
	 *          the chunk.
	 * @param offset
	 *          the file offset.
	 * @since 1.0
	 */

	private record ChunkOffset(
		Chunk	chunk,
		long	offset)
	{ }

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Member classes : inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: CHUNK READER


	/**
	 * This class implements a {@linkplain Chunk.IReader reader} for a chunk, which can be used by the chunk to read its
	 * data from the random-access file that was opened on a document.
	 *
	 * @since 1.0
	 * @see   Chunk#getReader()
	 * @see   Chunk#setReader(Chunk.IReader)
	 */

	private class ChunkReader
		implements Chunk.IReader
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	long	fileOffset;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		/**
		 * Creates a new instance of a chunk reader with the specified file offset.
		 *
		 * @param fileOffset
		 *          the offset to the start of the chunk data in the input file.
		 * @since 1.0
		 */

		private ChunkReader(
			long	fileOffset)
		{
			this.fileOffset = fileOffset;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : Chunk.IReader interface
	////////////////////////////////////////////////////////////////////

		/**
		 * Resets the chunk reader before any reading is performed, by seeking the start of the chunk data in the input
		 * file.
		 *
		 * @throws IOException
		 *           if an I/O error occurs.
		 * @since  1.0
		 */

		@Override
		public void reset()
			throws IOException
		{
			raFile.seek(fileOffset);
		}

		//--------------------------------------------------------------

		/**
		 * Returns the {@linkplain RandomAccessFile random-access file} from which the chunk data is read.
		 *
		 * @return the random-access file from which the chunk data will be read.
		 * @since  1.0
		 */

		@Override
		public DataInput getDataInput()
		{
			return raFile;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
