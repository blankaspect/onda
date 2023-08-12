/*====================================================================*\

IByteDataOutputStream.java

Byte data output stream interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

//----------------------------------------------------------------------


// BYTE DATA OUTPUT STREAM INTERFACE


public interface IByteDataOutputStream
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	void write(byte[] buffer,
			   int    offset,
			   int    length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
