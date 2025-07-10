/*====================================================================*\

ChunkList.java

Class: Nested-List File chunk list.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE CHUNK LIST


/**
 * This class implements a chunk list: a special {@linkplain Chunk chunk} that contains other chunks.  Objects of this
 * class serve as the branch nodes in the tree structure of a Nested-List File.  Lists may be nested: a list may contain
 * other lists, which gives rise to the name <i>Nested-List File</i>.  At the top level of the list hierarchy is the
 * <i>root list</i>, which contains everything in an NLF apart from the file header.
 * <p>
 * A list has the standard chunk header, with the reserved identifier {@code $LIST}, and <i>list-header extension</i>,
 * which consists of a <i>list-instance identifier</i> &mdash; an additional identifier that denotes the instance of a
 * list &mdash; and an optional <i>namespace name</i>.  In the list-header extension, the namespace name is encoded as
 * two size bytes followed by a UTF-8 sequence.  The byte order of the size bytes is determined by the byte-order flag
 * in the header of the Nested-List File {@link Document document} to which the list belongs.  The maximum size of a
 * namespace name is 65535 bytes.
 * </p>
 * <p>
 * A namespace name, which must be a well-formed URI reference, may be set on a list in order to declare a namespace
 * whose scope is the list itself and its descendants.  A namespace name that is set on a list is referred to as the
 * list's <i>local namespace name</i> to distinguish it from a list's namespace name, which is either its local
 * namespace name, or, if it has no local namespace name, the namespace name of its parent.
 * </p>
 * <p>
 * A list is created by the {@link Document#createList(Id)} method.  The root list of a document is created by the
 * {@link Document#createRootList(Id)} method.
 * </p>
 *
 * @since 1.0
 * @see   Chunk
 */

public class ChunkList
	extends Chunk
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The identifier of a {@linkplain ChunkList chunk list}. */
	public static final		Id		LIST_ID	= new Id(Id.RESERVED_PREFIX + "LIST");

	/** The size (in bytes) of the <i>namespace-name size</i> field of a chunk list. */
	public static final		int		NAMESPACE_NAME_SIZE_SIZE	= 2;

	/** The mask for the <i>namespace-name size</i> field of a chunk list. */
	protected static final	int		NAMESPACE_NAME_SIZE_MASK	= (1 << (NAMESPACE_NAME_SIZE_SIZE << 3)) - 1;

	/** The minimum size (in bytes) of the namespace name of a chunk list. */
	public static final		int		MIN_NAMESPACE_NAME_SIZE		= 0;

	/** The maximum size (in bytes) of the namespace name of a chunk list. */
	public static final		int		MAX_NAMESPACE_NAME_SIZE		= (1 << 16) - 1;

	/** The name of an XML <i>namespace</i> attribute. */
	private static final	String	XML_ATTR_NAME_XMLNS	= "xmlns";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Id			instanceId;
	private	String		namespaceName;
	private	List<Chunk>	chunks;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of a chunk list with the specified owner document and list-instance identifier.  The list
	 * will not have a local namespace name.
	 *
	 * @param document
	 *          the {@linkplain Document document} to which the list will belong.
	 * @param instanceId  the list-instance identifier.
	 * @since 1.0
	 * @see   ChunkList#ChunkList(Document, Id, String)
	 */

	protected ChunkList(
		Document	document,
		Id			instanceId)
	{
		// Call superclass constructor
		super(document, LIST_ID);

		// Initialise instance variables
		this.instanceId = instanceId;
		chunks = new ArrayList<>();
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of a chunk list with the specified owner document, list-instance identifier and local
	 * namespace name.
	 *
	 * @param  document
	 *           the {@linkplain Document document} to which the list will belong.
	 * @param  instanceId
	 *           the list-instance identifier.
	 * @param  namespaceName
	 *           the namespace name, which may be {@code null}.
	 * @throws IllegalArgumentException
	 *           <ul>
	 *             <li>{@code namespaceName} is not a well-formed URI reference, or</li>
	 *             <li>the length of the UTF-8 encoding of {@code namespaceName} is greater than 65535 bytes.</li>
	 *           </ul>
	 * @since  1.0
	 * @see    ChunkList#ChunkList(Document, Id)
	 */

	protected ChunkList(
		Document	document,
		Id			instanceId,
		String		namespaceName)
	{
		// Call alternative constructor
		this(document, instanceId);

		// Initialise instance variables
		setNamespaceName(namespaceName);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a string representation of this chunk list.  The string consists of the list-instance identifier and the
	 * local namespace name, if one has been set, separated with a comma and a space.
	 *
	 * @return a string consisting of the list-instance identifier and the local namespace name, if one has been set,
	 *         separated with a comma and a space.
	 * @since  1.0
	 */

	@Override
	public String toString()
	{
		return (namespaceName == null)
						? instanceId.toString()
						: instanceId.toString() + ", " + namespaceName.toString();
	}

	//------------------------------------------------------------------

	/**
	 * Updates the list's <i>size</i> field.  The size is set to the sum of the field size of the list-instance
	 * identifier, the field size of the namespace name and the sizes of the chunks in the list.  {@link
	 * Chunk#updateSize()} is called on each of the chunks in the list before its size is added to the size of this
	 * list.
	 *
	 * @since 1.0
	 */

	@Override
	public void updateSize()
	{
		long size = instanceId.getFieldSize() + getNamespaceNameFieldSize();
		for (Chunk chunk : chunks)
		{
			chunk.updateSize();
			size += chunk.getHeaderSize() + chunk.getSize();
		}
		setSize(size);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the namespace name that applies to this list, which is the list's local namespace name (ie, the namespace
	 * name that is set on the list), or, if the list does not have a local namespace name, the namespace name of the
	 * list's parent.  If none of the list's ancestors has a local namespace name, {@code null} is returned.
	 *
	 * @return the namespace name that applies to this list, or {@code null} if no namespace name applies.
	 * @since  1.0
	 * @see    #getLocalNamespaceName()
	 * @see    #setNamespaceName(String)
	 * @see    Chunk#getNamespaceName()
	 */

	@Override
	public String getNamespaceName()
	{
		return (namespaceName == null) ? super.getNamespaceName() : namespaceName;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the name of this list.  A list's name is formed by prefixing its namespace name and a vertical line
	 * character (U+007C) to its list-instance identifier.  If the list has no namespace name, its name is just its
	 * list-instance identifier.
	 *
	 * @return the name of this list.
	 * @since  1.0
	 * @see    Chunk#getName()
	 */

	@Override
	public String getName()
	{
		return instanceId.toName(getNamespaceName());
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @since  1.0
	 */

	@Override
	public String getPathname()
	{
		String prefix1 = "";
		String prefix2 = Character.toString(NlfConstants.PATHNAME_SEPARATOR_CHAR);
		ChunkList parent = getParent();
		if (parent == null)
		{
			if (isRoot())
				prefix2 = prefix1;
		}
		else
			prefix1 = parent.getPathname();
		return (prefix1 + prefix2 + instanceId.getValue());
	}

	//------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @since  1.0
	 */

	@Override
	public Element toXml(
		org.w3c.dom.Document	xmlDocument)
		throws DOMException, IOException, NlfException
	{
		return toXml(xmlDocument, null);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the list-instance identifier of this list.
	 *
	 * @return the list-instance identifier of this list.
	 * @since  1.0
	 */

	public Id getInstanceId()
	{
		return instanceId;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the local namespace name of this list (ie, the namespace name that is set on the list), or {@code null}
	 * if no namespace name is set on the list.
	 *
	 * @return the local namespace name of this list: the namespace name that is set on this list, or {@code null} if no
	 *         namespace name is set.
	 * @see    #getNamespaceName()
	 * @see    #setNamespaceName(String)
	 * @since  1.0
	 */

	public String getLocalNamespaceName()
	{
		return namespaceName;
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this list contains chunks.
	 *
	 * @return {@code true} if this list contains at least one chunk; {@code false} otherwise.
	 * @since  1.0
	 */

	public boolean hasChunks()
	{
		return !chunks.isEmpty();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the number of chunks in this list.
	 *
	 * @return the number of chunks in this list.
	 * @since  1.0
	 */

	public int getNumChunks()
	{
		return chunks.size();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the chunk at the specified index in this list.
	 *
	 * @param  index
	 *           the index of the required chunk in this list.
	 * @return the chunk at the {@code index} in this list.
	 * @throws IndexOutOfBoundsException
	 *           if {@code (index < 0)} or {@code (index >= }{@link #getNumChunks()}{@code )}.
	 * @since  1.0
	 * @see    #getNumChunks()
	 */

	public Chunk getChunk(
		int	index)
	{
		return chunks.get(index);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the attributes chunk of this list.
	 *
	 * @return the attributes chunk of this list, or {@code null} if the list does not have an attributes chunk.
	 * @since  1.0
	 * @see    #createAttributes()
	 */

	public Attributes getAttributes()
	{
		int index = firstIndexOf(Attributes.ATTRIBUTES_ID);
		return (index < 0) ? null : (Attributes)chunks.get(index);
	}

	//------------------------------------------------------------------

	/**
	 * Returns {@code true} if this list is the root list of the document to which it belongs.
	 *
	 * @return {@code true} if this list is the root list; {@code false} otherwise.
	 * @since  1.0
	 */

	public boolean isRoot()
	{
		return (getDocument().getRootList() == this);
	}

	//------------------------------------------------------------------

	/**
	 * Sets the local namespace name of this list to the specified string, which may be {@code null}.
	 *
	 * @param  nsName
	 *           the string to which the local namespace name of this list will be set.  If {@code nsName} is
	 *           {@code null} or an empty string, the list will not have a local namespace name.
	 * @throws IllegalArgumentException
	 *           if
	 *           <ul>
	 *             <li>{@code nsName} is not a well-formed URI reference, or</li>
	 *             <li>the length of the UTF-8 encoding of {@code nsName} is greater than 65535 bytes.</li>
	 *           </ul>
	 * @since  1.0
	 * @see    #getLocalNamespaceName()
	 * @see    #getNamespaceName()
	 */

	public void setNamespaceName(
		String	nsName)
	{
		// Replace empty name with null
		if ((nsName != null) && (nsName.length() == 0))
			nsName = null;

		// Validate name
		if (nsName != null)
		{
			try
			{
				new URI(nsName);
			}
			catch (URISyntaxException e)
			{
				throw new IllegalArgumentException();
			}

			if (!NlfUtils.isUtf8LengthWithinBounds(nsName, MIN_NAMESPACE_NAME_SIZE, MAX_NAMESPACE_NAME_SIZE))
			{
				throw new IllegalArgumentException();
			}
		}

		// Set name
		namespaceName = nsName;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of the local namespace name of this list when it is encoded as a UTF-8 sequence.  The size does
	 * not include the initial size bytes.
	 *
	 * @return the size of the local namespace name this list when it is encoded as a UTF-8 sequence, or {@code 0} if
	 *         the list does not have a local namespace name.
	 * @since  1.0
	 * @see    #getNamespaceNameFieldSize()
	 */

	public int getNamespaceNameSize()
	{
		return (namespaceName == null) ? 0 : NlfUtils.getUtf8Length(namespaceName);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the size of the field that the local namespace name of this list would occupy in a Nested-List File.  The
	 * size includes the initial size bytes and the UTF-8 encoding of the namespace name.
	 *
	 * @return the size of the UTF-8 encoding of the local namespace name of this list, including the initial size
	 *         bytes.
	 * @since  1.0
	 * @see    #getNamespaceNameSize()
	 */

	public int getNamespaceNameFieldSize()
	{
		return NAMESPACE_NAME_SIZE_SIZE + getNamespaceNameSize();
	}

	//------------------------------------------------------------------

	/**
	 * Returns the encoded form of the local namespace name of this list (two size bytes followed by a UTF-8 sequence)
	 * as an array of bytes.
	 *
	 * @param  littleEndian
	 *           {@code true} if the byte order of the size bytes is little-endian; {@code false} if the byte order is
	 *           big-endian.
	 * @return a byte array containing the encoded form of the list's local namespace name: two size bytes followed by a
	 *         UTF-8 sequence.
	 * @since  1.0
	 */

	public byte[] getNamespaceNameBytes(
		boolean	littleEndian)
	{
		byte[] bytes = null;
		int size = 0;
		if (namespaceName != null)
		{
			bytes = NlfUtils.stringToUtf8(namespaceName);
			size = bytes.length;
		}

		byte[] buffer = new byte[NAMESPACE_NAME_SIZE_SIZE + size];
		Utils.intToBytes(size, buffer, 0, NAMESPACE_NAME_SIZE_SIZE, littleEndian);
		if (bytes != null)
			System.arraycopy(bytes, 0, buffer, NAMESPACE_NAME_SIZE_SIZE, size);
		return buffer;
	}

	//------------------------------------------------------------------

	/**
	 * Adds the specified chunk to the end of this list.  An exception is thrown if the chunk does not belong to the
	 * same document as this list or if the inclusion of the chunk in this list would create a cycle in the list
	 * hierarchy.  If the chunk is already in the list, it will be removed and then added to the end of the list.
	 *
	 * @param  chunk
	 *           the chunk that will be added to the end of the list.
	 * @throws IllegalArgumentException
	 *           if {@code chunk} is {@code null}.
	 * @throws NlfUncheckedException
	 *           if
	 *           <ul>
	 *             <li>
	 *               this list and {@code chunk} belong to different documents, or
	 *             </li>
	 *             <li>
	 *               adding {@code chunk} to this list would create a cycle in the list hierarchy because {@code chunk}
	 *               is this list or an ancestor of this list.
	 *             </li>
	 *           </ul>
	 * @since  1.0
	 */

	public void appendChunk(
		Chunk	chunk)
	{
		// Test for null argument
		if (chunk == null)
			throw new IllegalArgumentException();

		// Test whether list and chunk belong to same document
		if (!getDocument().equals(chunk.getDocument()))
			throw new NlfUncheckedException(ExceptionId.DIFFERENT_DOCUMENTS);

		// Test whether chunk is this list or an ancestor of this list
		if (chunk.isList())
		{
			ChunkList list = this;
			while (list != null)
			{
				if (list == chunk)
					throw new NlfUncheckedException(ExceptionId.ADDING_ANCESTOR_NOT_ALLOWED);
				list = list.getParent();
			}
		}

		// If chunk is already in list, remove it
		for (int i = 0; i < chunks.size(); i++)
		{
			if (chunks.get(i) == chunk)
			{
				chunks.remove(i);
				break;
			}
		}

		// Set parent of chunk to this list, and add chunk to end of list
		chunk.setParent(this);
		chunks.add(chunk);
	}

	//------------------------------------------------------------------

	/**
	 * Removes the chunk at the specified index in this list, and returns the chunk that was removed.  The chunk that is
	 * returned has its <i>parent</i> field set to {@code null} to indicate that it has no parent.
	 *
	 * @param  index
	 *           the index of the chunk that will be removed from this list.
	 * @return the chunk that was removed from this list.
	 * @throws IndexOutOfBoundsException
	 *           if {@code (index < 0)} or {@code (index >= }{@link #getNumChunks()}{@code)}.
	 * @since  1.0
	 * @see    #appendChunk(Chunk)
	 * @see    #getNumChunks()
	 */

	public Chunk removeChunk(
		int	index)
	{
		Chunk chunk = chunks.remove(index);
		chunk.setParent(null);
		return chunk;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the index of the first occurrence of a chunk with the specified identifier in this list.
	 *
	 * @param  id
	 *           the identifier of the chunk that will be searched for.
	 * @return the index of the first occurrence of a chunk with the identifier {@code id} in this list, or {@code -1}
	 *         if no such chunk was found.
	 * @since  1.0
	 * @see    #firstIndexOf(Id, int)
	 * @see    #lastIndexOf(Id)
	 * @see    #lastIndexOf(Id, int)
	 */

	public int firstIndexOf(
		Id	id)
	{
		return firstIndexOf(id, 0);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the index of the first occurrence of a chunk with the specified identifier in this list, starting at the
	 * specified index.
	 *
	 * @param  id
	 *           the identifier of the chunk that will be searched for.
	 * @param  startIndex  the index at which the search will start.  The index may be outside the bounds of the list.
	 * @return the index of the first occurrence of a chunk with the identifier {@code id} in this list, starting at
	 *         {@code startIndex}, or {@code -1} if no such chunk was found.
	 * @since  1.0
	 * @see    #firstIndexOf(Id)
	 * @see    #lastIndexOf(Id)
	 * @see    #lastIndexOf(Id, int)
	 */

	public int firstIndexOf(
		Id	id,
		int	startIndex)
	{
		for (int i = Math.max(0, startIndex); i < chunks.size(); i++)
		{
			if (chunks.get(i).getId().equals(id))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the index of the last occurrence of a chunk with the specified identifier in this list.
	 *
	 * @param  id
	 *           the identifier of the chunk that will be searched for.
	 * @return the index of the last occurrence of a chunk with the identifier {@code id} in this list, or {@code -1} if
	 *         no such chunk was found.
	 * @since  1.0
	 * @see    #firstIndexOf(Id)
	 * @see    #firstIndexOf(Id, int)
	 * @see    #lastIndexOf(Id, int)
	 */

	public int lastIndexOf(
		Id	id)
	{
		return lastIndexOf(id, chunks.size() - 1);
	}

	//------------------------------------------------------------------

	/**
	 * Returns the index of the last occurrence of a chunk with the specified identifier in this list, ending at the
	 * specified index.
	 *
	 * @param  id
	 *           the identifier of the chunk that will be searched for.
	 * @param  endIndex
	 *           the index at which the search will end.  The index may be outside the bounds of the list.
	 * @return the index of the last occurrence of a chunk with the identifier {@code id} in this list, ending at {@code
	 *         endIndex}, or {@code -1} if no such chunk was found.
	 * @since  1.0
	 * @see    #firstIndexOf(Id)
	 * @see    #firstIndexOf(Id, int)
	 * @see    #lastIndexOf(Id)
	 */

	public int lastIndexOf(
		Id	id,
		int	endIndex)
	{
		for (int i = Math.min(endIndex, chunks.size() - 1); i >= 0; i--)
		{
			if (chunks.get(i).getId().equals(id))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	/**
	 * Creates an {@linkplain Attributes attributes chunk} for this list, if the list does not already have one.  If an
	 * attributes chunk is created, it is returned; otherwise {@code null} is returned.  An attributes chunk is added at
	 * the start of the list, so that it is always the first chunk in the list.
	 *
	 * @return the attributes chunk that was created for this list, or {@code null} if the list already has an
	 *         attributes chunk.
	 * @since  1.0
	 */

	public Attributes createAttributes()
	{
		if (firstIndexOf(Attributes.ATTRIBUTES_ID) < 0)
		{
			Attributes attributes = new Attributes(getDocument());
			attributes.setParent(this);
			chunks.add(0, attributes);
			return attributes;
		}
		return null;
	}

	//------------------------------------------------------------------

	/**
	 * Writes the list-header extension (the list-instance identifier and namespace name) to the specified {@linkplain
	 * DataOutput data output}.
	 *
	 * @param  dataOutput
	 *           the data output to which the list-header extension will be written.
	 * @throws IOException
	 *           if an error occurs when writing the list-header extension to the data output.
	 * @since  1.0
	 */

	public void writeHeaderExtension(
		DataOutput	dataOutput)
		throws IOException
	{
		// Write list-instance identifier
		instanceId.write(dataOutput);

		// Write namespace name
		dataOutput.write(getNamespaceNameBytes(getDocument().isLittleEndian()));
	}

	//------------------------------------------------------------------

	/**
	 * Traverses the document subtree in the specified order, starting at this list and calling the specified chunk
	 * processor on each chunk that is visited.
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
		if ((traversalOrder == null) || (processor == null))
			throw new IllegalArgumentException();

		switch (traversalOrder)
		{
			case BREADTH_FIRST:
			{
				// Initialise queue
				Deque<Chunk> queue = new ArrayDeque<>(32);

				// Add this list to queue
				queue.addLast(this);

				// While there are chunks left to process ...
				while (!queue.isEmpty())
				{
					// Get next chunk from queue
					Chunk chunk = queue.removeFirst();

					// If chunk is list, add its chunks to queue ...
					if (chunk.isList())
					{
						for (Chunk child : ((ChunkList)chunk).chunks)
							queue.addLast(child);
					}

					// ... otherwise, if chunk is not attributes, process it
					else if (!chunk.isAttributes())
						processor.process(chunk);
				}
				break;
			}

			case DEPTH_FIRST:
			{
				// Process chunk-list children
				for (Chunk chunk : chunks)
				{
					if (chunk.isList())
						((ChunkList)chunk).processChunks(traversalOrder, processor);
				}

				// Process children that are neither attributes nor chunk lists
				for (Chunk chunk : chunks)
				{
					if (!chunk.isAttributes() && !chunk.isList())
						processor.process(chunk);
				}
				break;
			}
		}
	}

	//------------------------------------------------------------------

	/**
	 * Generates an XML element of the specified XML document from this list and returns the result.
	 *
	 * @param  xmlDocument
	 *           the XML document that will be the owner of the element that is created.
	 * @param  element
	 *           the XML element that will be generated, or {@code null} if a new element should be created.
	 * @return the XML element that is generated from this list.
	 * @throws DOMException
	 *           if an exception occurs when creating the XML element or one of its child elements.
	 * @throws IOException
	 *           if an I/O error occurs when reading chunk data.
	 * @throws NlfException
	 *           if
	 *           <ul>
	 *             <li>an exception occurs when encoding chunk data, or</li>
	 *             <li>there is not enough memory to create an XML element.</li>
	 *           </ul>
	 * @since  1.0
	 */

	protected Element toXml(
		org.w3c.dom.Document	xmlDocument,
		Element					element)
		throws DOMException, IOException, NlfException
	{
		if (element == null)
			element = xmlDocument.createElement(instanceId.getValue());

		if (namespaceName != null)
			element.setAttribute(XML_ATTR_NAME_XMLNS, namespaceName);

		for (Chunk chunk : chunks)
		{
			if (chunk.isAttributes())
				((Attributes)chunk).toXml(element);
			else
				element.appendChild(chunk.toXml(xmlDocument));
		}
		return element;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
