/*====================================================================*\

AppConstants.java

Application constants interface.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Insets;

//----------------------------------------------------------------------


// APPLICATION CONSTANTS INTERFACE


interface AppConstants
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	// Component constants
	Insets	COMPONENT_INSETS	= new Insets(2, 3, 2, 3);

	// Strings
	String	ELLIPSIS_STR		= "...";
	String	OK_STR				= "OK";
	String	CANCEL_STR			= "Cancel";
	String	CONTINUE_STR		= "Continue";
	String	REPLACE_STR			= "Replace";
	String	ALREADY_EXISTS_STR	= "\nThe file already exists.\nDo you want to replace it?";

	// Filename suffixes
	String		AIFF_FILE_SUFFIX1		= ".aif";
	String		AIFF_FILE_SUFFIX2		= ".aiff";
	String		COMPRESSED_FILE_SUFFIX	= ".onda";
	String		WAVE_FILE_SUFFIX1		= ".wav";
	String		WAVE_FILE_SUFFIX2		= ".wave";
	String		XML_FILE_SUFFIX			= ".xml";
	String[]	AUDIO_FILE_SUFFIXES		= { AIFF_FILE_SUFFIX1, AIFF_FILE_SUFFIX2,
											WAVE_FILE_SUFFIX1, WAVE_FILE_SUFFIX2 };

	// File-filter descriptions
	String	AUDIO_FILES_STR			= "Audio files";
	String	COMPRESSED_FILES_STR	= "Compressed audio files";
	String	XML_FILES_STR			= "XML files";

}

//----------------------------------------------------------------------
