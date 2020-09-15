/*====================================================================*\

Log.java

Log class.

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


// LOG CLASS


class Log
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	Log	INSTANCE	= new Log();

	public static final	String	ERROR_PREFIX	= "! ";

////////////////////////////////////////////////////////////////////////
//  Member classes : non-inner classes
////////////////////////////////////////////////////////////////////////


	// LOG LINE CLASS


	public static class Line
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		enum Kind
		{
			INFO,
			ERROR
		}

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Line(Kind   kind,
					 String str)
		{
			this.kind = kind;
			this.str = str;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		Kind	kind;
		String	str;

	}

	//==================================================================

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

	public void setShow(boolean show)
	{
		this.show = show;
	}

	//------------------------------------------------------------------

	public void appendLine(String str)
	{
		if (show)
			System.out.println(str);
		else
			lines.add(new Line(Line.Kind.INFO, str));
	}

	//------------------------------------------------------------------

	public void appendException(AppException exception)
	{
		for (String str : StringUtils.split(exception.toString(), '\n'))
		{
			if (show)
				System.out.println(ERROR_PREFIX + str);
			else
				lines.add(new Line(Line.Kind.ERROR, str));
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	List<Line>	lines;
	private	boolean		show;

}

//----------------------------------------------------------------------
