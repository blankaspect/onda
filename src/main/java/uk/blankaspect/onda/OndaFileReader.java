/*====================================================================*\

OndaFileReader.java

Onda file reader interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.misc.IByteDataOutputStream;

//----------------------------------------------------------------------


// ONDA FILE READER INTERFACE


interface OndaFileReader
{

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// ONDA FILE READER KIND


	enum Kind
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		IFF
		{
			@Override
			public OndaFileReader createReader(File file)
			{
				return new OndaFileIff(file);
			}
		},

		NLF
		{
			@Override
			public OndaFileReader createReader(File file)
			{
				return new OndaFile(file);
			}
		};

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Kind()
		{
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Abstract methods
	////////////////////////////////////////////////////////////////////

		public abstract OndaFileReader createReader(File file);

		//--------------------------------------------------------------

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Methods
////////////////////////////////////////////////////////////////////////

	byte[] getPrivateData();

	//------------------------------------------------------------------

	OndaFile.Attributes readAttributes()
		throws AppException;

	//------------------------------------------------------------------

	PrivateData readPrivateData()
		throws AppException;

	//------------------------------------------------------------------

	OndaFile.Attributes readAttributesAndPrivateData()
		throws AppException;

	//------------------------------------------------------------------

	OndaFile.Attributes readData(IByteDataOutputStream outStream)
		throws AppException;

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
