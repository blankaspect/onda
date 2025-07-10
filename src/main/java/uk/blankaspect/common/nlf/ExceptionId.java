/*====================================================================*\

ExceptionId.java

Enumeration: identifiers of Nested-List File exceptions.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// ENUMERATION: IDENTIFIERS OF NESTED-LIST FILE EXCEPTIONS


/**
 * This is an enumeration of the identifiers of Nested-List File exceptions, {@link NlfException} and {@link
 * NlfUncheckedException}.  Each identifier is associated with a message.
 *
 * @since  1.0
 */

public enum ExceptionId
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Checked exceptions

	FAILED_TO_OPEN_FILE
	("Failed to open the file."),

	FAILED_TO_CLOSE_FILE
	("Failed to close the file."),

	FAILED_TO_LOCK_FILE
	("Failed to lock the file."),

	ERROR_READING_FILE
	("An error occurred when reading the file."),

	ERROR_WRITING_FILE
	("An error occurred when writing the file."),

	FILE_ACCESS_NOT_PERMITTED
	("Access to the file was not permitted."),

	FAILED_TO_CREATE_DIRECTORY
	("Failed to create the directory."),

	FAILED_TO_CREATE_TEMPORARY_FILE
	("Failed to create a temporary file."),

	FAILED_TO_DELETE_FILE
	("Failed to delete the existing file."),

	FAILED_TO_RENAME_FILE
	("Failed to rename the temporary file."),

	NOT_A_NESTED_LIST_FILE
	("The file is not a Nested-List File."),

	INVALID_VERSION_NUM
	("The version number of the file is invalid."),

	UNSUPPORTED_VERSION
	("The version of the file is not supported."),

	NO_ROOT_LIST
	("The file does not have a root list."),

	MALFORMED_FILE
	("The file is malformed."),

	INVALID_CHUNK_ID
	("The chunk identifier is invalid."),

	CHUNK_SIZE_OUT_OF_BOUNDS
	("The chunk size is out of bounds."),

	INVALID_LIST_INSTANCE_ID
	("The list-instance identifier is invalid."),

	INVALID_NAMESPACE_NAME
	("The namespace name is invalid."),

	MULTIPLE_ATTRIBUTES_CHUNKS
	("The list has more than one attributes chunk."),

	MALFORMED_ATTRIBUTES_CHUNK
	("The attributes chunk is malformed."),

	INVALID_ATTRIBUTE_NAME
	("The attribute name is invalid."),

	INVALID_ATTRIBUTE_VALUE
	("The attribute value is invalid."),

	MULTIPLE_ATTRIBUTES
	("The attributes chunk has more than one attribute with the same name."),

	FAILED_TO_CREATE_XML_DOCUMENT
	("Failed to create an XML document."),

	ERROR_GENERATING_XML_DOCUMENT
	("An error occurred in generating an XML document."),

	OPERATION_TERMINATED
	("The operation was terminated."),

	NOT_ENOUGH_MEMORY
	("There was not enough memory to perform the command."),


	// Unchecked exceptions

	RESERVED_IDENTIFIER
	("The identifier is reserved."),

	FILE_IS_OPEN_ON_DOCUMENT
	("A file is already open on the document."),

	DIFFERENT_DOCUMENTS
	("The chunk and the list belong to different documents."),

	ADDING_ANCESTOR_NOT_ALLOWED
	("Adding an ancestor of a list to a list is not allowed."),

	ERROR_ENCODING_CHUNK_DATA
	("An error occurred in encoding the chunk data.");

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	String	message;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new enumeration constant for an exception identifier.
	 *
	 * @param message
	 *          the message that is associated with the exception identifier.
	 * @since 1.0
	 */

	private ExceptionId(
		String	message)
	{
		this.message = message;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the message that is associated with this exception identifier.
	 *
	 * @return the message that is associated with this exception identifier.
	 * @since  1.0
	 */

	public String getMessage()
	{
		return message;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
