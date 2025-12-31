/*====================================================================*\

FPathnameComboBox.java

Pathname combo box class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import uk.blankaspect.ui.swing.combobox.PathnameComboBox;

//----------------------------------------------------------------------


// PATHNAME COMBO BOX CLASS


class FPathnameComboBox
	extends PathnameComboBox
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	MAX_NUM_PATHNAMES	= 32;
	private static final	int	NUM_COLUMNS			= 40;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public FPathnameComboBox()
	{
		super(MAX_NUM_PATHNAMES, NUM_COLUMNS);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
