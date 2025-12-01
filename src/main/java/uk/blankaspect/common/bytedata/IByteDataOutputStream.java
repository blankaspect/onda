/*====================================================================*\

IByteDataOutputStream.java

Interface: byte-data output stream.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.bytedata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

//----------------------------------------------------------------------


// INTERFACE: BYTE-DATA OUTPUT STREAM


public interface IByteDataOutputStream
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	void write(
		byte[]	buffer,
		int		offset,
		int		length)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
