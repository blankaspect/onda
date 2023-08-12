/*====================================================================*\

IDoubleDataSource.java

Double-data source interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.doubledata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// DOUBLE-DATA SOURCE INTERFACE


public interface IDoubleDataSource
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// DOUBLE DATA CLASS


	public static class DoubleData
	{

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public DoubleData(double[] data)
		{
			this.data = data;
			length = data.length;
		}

		//--------------------------------------------------------------

		public DoubleData(double[] data,
						  int      offset,
						  int      length)
		{
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	double[]	data;
		public	int			offset;
		public	int			length;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	DoubleData getData()
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
