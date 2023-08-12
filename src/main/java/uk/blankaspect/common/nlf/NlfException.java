/*====================================================================*\

NlfException.java

Class: Nested-List File exception.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

//----------------------------------------------------------------------


// CLASS: NESTED-LIST FILE EXCEPTION


/**
 * An object of this class signals that an exception has occurred during an operation on a Nested-List File.  An
 * exception has an identifier that is associated with a detail message, and may include the file on which the exception
 * occurred and the offset within the file at which it occurred.  The exception may also act as a wrapper for another
 * {@code Throwable}, which is known as the <i>cause</i>.
 * <p>
 * The file, offset and cause are not automatically incorporated into the detail message of an {@code NlfException}, but
 * they can be obtained, along with the exception identifier, by methods of this class or a superclass.
 * </p>
 *
 * @since 1.0
 * @see   NlfUncheckedException
 */

public class NlfException
	extends Exception
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of an exception with the specified identifier.  The identifier is associated with a
	 * message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id  the identifier of the exception.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id)
	{
		// Call superclass constructor
		super(id.getMessage());

		// Initialise instance variables
		this.id = id;
		offset = -1;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an exception with the specified identifier and cause.  The identifier is associated
	 * with a message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id     the identifier of the exception.
	 * @param cause  the underlying cause of the exception.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id,
						Throwable   cause)
	{
		// Call superclass constructor
		super(id.getMessage(), cause);

		// Initialise instance variables
		this.id = id;
		offset = -1;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an exception with the specified identifier and file.  The identifier is associated with
	 * a message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id    the identifier of the exception.
	 * @param file  the file that is associated with the exception, usually the file on which the exception occurred.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id,
						File        file)
	{
		// Call alternative constructor
		this(id, file, -1);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an exception with the specified identifier, file and cause.  The identifier is
	 * associated with a message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id     the identifier of the exception.
	 * @param file   the file that is associated with the exception, usually the file on which the exception occurred.
	 * @param cause  the underlying cause of the exception.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id,
						File        file,
						Throwable   cause)
	{
		// Call alternative constructor
		this(id, file, -1, cause);
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an exception with the specified identifier, file and offset.  The identifier is
	 * associated with a message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id      the identifier of the exception.
	 * @param file    the file that is associated with the exception, usually the file on which the exception occurred.
	 * @param offset  the offset that is associated with the exception, usually the offset to <b>{@code file}</b> at
	 *                which the exception occurred.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id,
						File        file,
						long        offset)
	{
		// Call superclass constructor
		super(id.getMessage());

		// Initialise instance variables
		this.id = id;
		this.file = file;
		this.offset = (offset < 0) ? -1 : offset;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an exception with the specified identifier, file, offset and cause.  The identifier is
	 * associated with a message that is used as the detail message in the constructor of {@link Exception}.
	 *
	 * @param id      the identifier of the exception.
	 * @param file    the file that is associated with the exception, usually the file on which the exception occurred.
	 * @param offset  the offset that is associated with the exception, usually the offset to <b>{@code file}</b> at
	 *                which the exception occurred.
	 * @param cause   the underlying cause of the exception.
	 * @since 1.0
	 */

	public NlfException(ExceptionId id,
						File        file,
						long        offset,
						Throwable   cause)
	{
		// Call superclass constructor
		super(id.getMessage(), cause);

		// Initialise instance variables
		this.id = id;
		this.file = file;
		this.offset = (offset < 0) ? -1 : offset;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the identifier of this exception.
	 *
	 * @return the identifier of this exception.
	 * @since  1.0
	 */

	public ExceptionId getId()
	{
		return id;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the file that is associated with this exception.  The file will usually be the file on which the
	 * exception occurred.
	 *
	 * @return the file that is associated with this exception, or {@code null} if there is no associated file.
	 * @since  1.0
	 * @see    #getOffset()
	 */

	public File getFile()
	{
		return file;
	}

	//------------------------------------------------------------------

	/**
	 * Returns the offset that is associated with this exception.  The offset will usually be an offset to the
	 * {@linkplain #getFile() file} at which the exception occurred.
	 *
	 * @return the offset that is associated with this exception.  A value of {@code -1} indicates that the offset has
	 *         not been set, or that it was set to an illegal value.
	 * @since  1.0
	 * @see    #getFile()
	 */

	public long getOffset()
	{
		return offset;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	ExceptionId	id;
	private	File		file;
	private	long		offset;

}

//----------------------------------------------------------------------
