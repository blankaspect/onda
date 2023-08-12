/*====================================================================*\

IDoubleDataOutputStream.java

Double data output stream interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.doubledata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

//----------------------------------------------------------------------


// DOUBLE DATA OUTPUT STREAM INTERFACE


public interface IDoubleDataOutputStream
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	void write(double[] buffer,
			   int      offset,
			   int      length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
