/*====================================================================*\

IDoubleDataSource.java

Interface: double-data source.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.doubledata;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IDataInput;

//----------------------------------------------------------------------


// INTERFACE: DOUBLE-DATA SOURCE


public interface IDoubleDataSource
	extends IDataInput
{

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	DoubleData getData()
		throws AppException;

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// CLASS: DOUBLE DATA


	public static class DoubleData
	{

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		public	double[]	data;
		public	int			offset;
		public	int			length;

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		public DoubleData(
			double[]	data)
		{
			this.data = data;
			length = data.length;
		}

		//--------------------------------------------------------------

		public DoubleData(
			double[]	data,
			int			offset,
			int			length)
		{
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
