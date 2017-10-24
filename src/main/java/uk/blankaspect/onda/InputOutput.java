/*====================================================================*\

InputOutput.java

Input-output file pair class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

//----------------------------------------------------------------------


// INPUT-OUTPUT FILE PAIR CLASS


class InputOutput
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public InputOutput(File input)
	{
		this.input = input;
	}

	//------------------------------------------------------------------

	public InputOutput(File input,
					   File output)
	{
		this.input = input;
		this.output = output;
	}

	//------------------------------------------------------------------

	public InputOutput(File        input,
					   InputOutput inputOutput)
	{
		this.input = input;
		inputRootDirectory = inputOutput.inputRootDirectory;
		output = inputOutput.output;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void updateRootDirectory()
	{
		inputRootDirectory = input;
	}

	//------------------------------------------------------------------

	public File getOutputDirectory()
	{
		File outDirectory = output;
		if (outDirectory == null)
			outDirectory = input.getAbsoluteFile().getParentFile();
		else
		{
			if (inputRootDirectory != null)
			{
				String inPathname = input.getParent();
				if (inPathname == null)
					inPathname = "";
				String inRootPathname = inputRootDirectory.getPath();
				if (inPathname.length() > inRootPathname.length())
					outDirectory = new File(output, inPathname.substring(inRootPathname.length()));
			}
		}
		return outDirectory;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance fields
////////////////////////////////////////////////////////////////////////

	File	input;
	File	inputRootDirectory;
	File	output;

}

//----------------------------------------------------------------------
