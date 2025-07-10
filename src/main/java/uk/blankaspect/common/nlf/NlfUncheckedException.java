/*====================================================================*\

NlfUncheckedException.java

Class: unchecked Nested-List File exception.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// CLASS: UNCHECKED NESTED-LIST FILE EXCEPTION


/**
 * An object of this class signals that an exception has occurred in a method of a class in this package.  Like the
 * checked {@link NlfException}, this unchecked exception is constructed with an identifier that is associated with a
 * detail message.  It is intended as a more specific replacement for a runtime exception such as {@code
 * IllegalArgumentException} or {@code IllegalStateException}.
 *
 * @since 1.0
 * @see   NlfException
 */

public class NlfUncheckedException
	extends RuntimeException
{

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	ExceptionId	id;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new instance of an unchecked exception with the specified identifier.  The identifier is associated
	 * with a message that is used as the detail message in the constructor of {@link RuntimeException}.
	 *
	 * @param id
	 *          the identifier of the exception.
	 * @since 1.0
	 */

	public NlfUncheckedException(
		ExceptionId	id)
	{
		// Call superclass constructor
		super(id.getMessage());

		// Initialise instance variables
		this.id = id;
	}

	//------------------------------------------------------------------

	/**
	 * Creates a new instance of an unchecked exception with the specified identifier and cause.  The identifier is
	 * associated with a message that is used as the detail message in the constructor of {@link RuntimeException}.
	 *
	 * @param id
	 *          the identifier of the exception.
	 * @param cause
	 *          the underlying cause of the exception.
	 * @since 1.0
	 */

	public NlfUncheckedException(
		ExceptionId	id,
		Throwable	cause)
	{
		// Call superclass constructor
		super(id.getMessage(), cause);

		// Initialise instance variables
		this.id = id;
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

}

//----------------------------------------------------------------------
