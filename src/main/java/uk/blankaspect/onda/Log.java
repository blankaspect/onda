/*====================================================================*\

Log.java

Class: log.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.string.StringUtils;

//----------------------------------------------------------------------


// CLASS: LOG


class Log
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	Log		INSTANCE	= new Log();

	public static final	String	ERROR_PREFIX	= "! ";

	enum LineKind
	{
		INFO,
		ERROR
	}

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<Line>	lines;
	private	boolean		show;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private Log()
	{
		lines = new ArrayList<>();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public List<Line> getLines()
	{
		return Collections.unmodifiableList(lines);
	}

	//------------------------------------------------------------------

	public boolean isEmpty()
	{
		return lines.isEmpty();
	}

	//------------------------------------------------------------------

	public void clear()
	{
		lines.clear();
	}

	//------------------------------------------------------------------

	public void setShow(
		boolean	show)
	{
		this.show = show;
	}

	//------------------------------------------------------------------

	public void appendLine(
		String	text)
	{
		if (show)
			System.out.println(text);
		else
			lines.add(Line.info(text));
	}

	//------------------------------------------------------------------

	public void appendException(
		AppException	exception)
	{
		for (String str : StringUtils.split(exception.toString(), '\n'))
		{
			if (show)
				System.out.println(ERROR_PREFIX + str);
			else
				lines.add(Line.error(str));
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Member records
////////////////////////////////////////////////////////////////////////


	// RECORD: LINE OF LOG


	public record Line(
		LineKind	kind,
		String		text)
	{

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		private static Line info(
			String	text)
		{
			return new Line(LineKind.INFO, text);
		}

		//--------------------------------------------------------------

		private static Line error(
			String	text)
		{
			return new Line(LineKind.ERROR, text);
		}

		//--------------------------------------------------------------

	}

	//==================================================================

}

//----------------------------------------------------------------------
