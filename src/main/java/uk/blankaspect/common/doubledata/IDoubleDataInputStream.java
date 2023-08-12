/*====================================================================*\

IDoubleDataInputStream.java

Double data input stream interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.doubledata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// DOUBLE DATA INPUT STREAM INTERFACE


public interface IDoubleDataInputStream
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	int read(double[] buffer,
			 int      offset,
			 int      length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
