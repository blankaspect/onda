/*====================================================================*\

AppConstants.java

Interface: application constants.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.awt.Insets;

import uk.blankaspect.common.misc.FilenameSuffixFilter;

//----------------------------------------------------------------------


// INTERFACE: APPLICATION CONSTANTS


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

	// Filename extensions
	String	AIFF_FILENAME_EXTENSION1		= ".aif";
	String	AIFF_FILENAME_EXTENSION2		= ".aiff";
	String	COMPRESSED_FILENAME_EXTENSION	= ".onda";
	String	WAVE_FILENAME_EXTENSION1		= ".wav";
	String	WAVE_FILENAME_EXTENSION2		= ".wave";
	String	XML_FILENAME_EXTENSION			= ".xml";
	String[]	AUDIO_FILENAME_EXTENSIONS	=
			{ AIFF_FILENAME_EXTENSION1, AIFF_FILENAME_EXTENSION2, WAVE_FILENAME_EXTENSION1, WAVE_FILENAME_EXTENSION2 };

	// Filters for file choosers
	FilenameSuffixFilter AUDIO_FILE_FILTER		=
			new FilenameSuffixFilter("Audio files", AUDIO_FILENAME_EXTENSIONS);
	FilenameSuffixFilter COMPRESSED_FILE_FILTER	=
			new FilenameSuffixFilter("Compressed audio files", COMPRESSED_FILENAME_EXTENSION);
	FilenameSuffixFilter XML_FILE_FILTER		=
			new FilenameSuffixFilter("XML files", XML_FILENAME_EXTENSION);

}

//----------------------------------------------------------------------
