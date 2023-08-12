/*====================================================================*\

IDataInput.java

interface: data input.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.misc;

//----------------------------------------------------------------------


// INTERFACE: DATA INPUT


public interface IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	long getLength();

	//------------------------------------------------------------------

	void reset();

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ENUMERATION: DATA-INPUT KIND


	public enum Kind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		BYTE_STREAM,
		BYTE_SOURCE,
		DOUBLE_STREAM,
		DOUBLE_SOURCE;

	////////////////////////////////////////////////////////////////////
	//  Instance methods
	////////////////////////////////////////////////////////////////////

		public boolean isByteInput()
		{
			return (this == BYTE_STREAM) || (this == BYTE_SOURCE);
		}

		//--------------------------------------------------------------

		public boolean isDoubleInput()
		{
			return (this == DOUBLE_STREAM) || (this == DOUBLE_SOURCE);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------