/*====================================================================*\

NlfConstants.java

Interface: published Nested-List File constants.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.nlf;

//----------------------------------------------------------------------


// INTERFACE: PUBLISHED NESTED-LIST FILE CONSTANTS


/**
 * This interface defines publicly accessible constants that are used by classes in the Nested-List File package.
 *
 * @since 1.0
 */

public interface NlfConstants
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	/** The character that separates the prefix of an {@linkplain Id identifier} of a {@linkplain Chunk chunk} from the
		value of the identifier. */
	char	NAME_SEPARATOR_CHAR		= '|';

	/** The character that separates the elements of a pathname of a {@linkplain Chunk chunk} within a Nested-List
		File. */
	char	PATHNAME_SEPARATOR_CHAR	= '.';

	/** This is an enumeration of the orders in which the chunks of a Nested-List File may be traversed. */
	enum TraversalOrder
	{
		/** Breadth-first traversal. */
		BREADTH_FIRST,

		/** Depth-first traversal. */
		DEPTH_FIRST
	}

}

//----------------------------------------------------------------------
