/*====================================================================*\

Utils.java

Utility methods class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import java.util.List;

import uk.blankaspect.common.config.PropertiesPathname;

import uk.blankaspect.common.exception2.ExceptionUtils;

import uk.blankaspect.common.filesystem.PathnameUtils;

//----------------------------------------------------------------------


// UTILITY METHODS CLASS


class Utils
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	String	FAILED_TO_GET_PATHNAME_STR	= "Failed to get the canonical pathname for ";

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private Utils()
	{
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int indexOf(Object   target,
							  Object[] values)
	{
		for (int i = 0; i < values.length; i++)
		{
			if (values[i].equals(target))
				return i;
		}
		return -1;
	}

	//------------------------------------------------------------------

	public static String stripTrailingSpace(String str)
	{
		int length = str.length();
		int index = length;
		while (index > 0)
		{
			char ch = str.charAt(--index);
			if ((ch != '\t') && (ch != ' '))
			{
				++index;
				break;
			}
		}
		return (index < length) ? str.substring(0, index) : str;
	}

	//------------------------------------------------------------------

	public static String listToString(List<? extends Object> items)
	{
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < items.size(); i++)
		{
			if (i > 0)
				buffer.append(", ");
			buffer.append('"');
			buffer.append(items.get(i));
			buffer.append('"');
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

	public static char getFileSeparatorChar()
	{
		return AppConfig.INSTANCE.isShowUnixPathnames() ? '/' : File.separatorChar;
	}

	//------------------------------------------------------------------

	public static String getPathname(File file)
	{
		return getPathname(file, AppConfig.INSTANCE.isShowUnixPathnames());
	}

	//------------------------------------------------------------------

	public static String getPathname(File    file,
									 boolean unixStyle)
	{
		String pathname = null;
		if (file != null)
		{
			try
			{
				pathname = file.getCanonicalPath();
			}
			catch (Exception e)
			{
				ExceptionUtils.printStderrLocated(FAILED_TO_GET_PATHNAME_STR + file.getPath());
				System.err.println("- " + e);
				pathname = file.getAbsolutePath();
			}

			if (unixStyle)
				pathname = PathnameUtils.toUnixStyle(pathname, true);
		}
		return pathname;
	}

	//------------------------------------------------------------------

	public static String getPropertiesPathname()
	{
		String pathname = PropertiesPathname.getPathname();
		if (pathname != null)
			pathname += OndaApp.NAME_KEY;
		return pathname;
	}

	//------------------------------------------------------------------

	public static File appendSuffix(File   file,
									String suffix)
	{
		String filename = file.getName();
		if (!filename.isEmpty() && (filename.indexOf('.') < 0))
			file = new File(file.getParentFile(), filename + suffix);
		return file;
	}

	//------------------------------------------------------------------

	public static String[] getOptionStrings(String... optionStrs)
	{
		String[] strs = new String[optionStrs.length + 1];
		System.arraycopy(optionStrs, 0, strs, 0, optionStrs.length);
		strs[optionStrs.length] = AppConstants.CANCEL_STR;
		return strs;
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
