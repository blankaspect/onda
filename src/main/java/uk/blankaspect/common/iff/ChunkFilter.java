/*====================================================================*\

ChunkFilter.java

Chunk filter class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// CHUNK FILTER CLASS


public class ChunkFilter
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	ChunkFilter	INCLUDE_ALL	= new ChunkFilter(Kind.EXCLUDE);
	public static final	ChunkFilter	EXCLUDE_ALL	= new ChunkFilter(Kind.INCLUDE);

	public static final	char	DEFAULT_SEPARATOR	= ',';

	private static final	String	INCLUDE_ALL_STR	= "<include all>";
	private static final	String	EXCLUDE_ALL_STR	= "<exclude all>";

////////////////////////////////////////////////////////////////////////
//  Enumerated types
////////////////////////////////////////////////////////////////////////


	// FILTER KIND


	public enum Kind
		implements IStringKeyed
	{

	////////////////////////////////////////////////////////////////////
	//  Constants
	////////////////////////////////////////////////////////////////////

		INCLUDE ("+",  "Include"),
		EXCLUDE ("-",  "Exclude");

	////////////////////////////////////////////////////////////////////
	//  Constructors
	////////////////////////////////////////////////////////////////////

		private Kind(String key,
					 String text)
		{
			this.key = key;
			this.text = text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Class methods
	////////////////////////////////////////////////////////////////////

		public static Kind get(int index)
		{
			return ((index >= 0) && (index < values().length)) ? values()[index] : null;
		}

		//--------------------------------------------------------------

		public static Kind forKey(String key)
		{
			for (Kind value : values())
			{
				if (value.key.equals(key))
					return value;
			}
			return null;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : IStringKeyed interface
	////////////////////////////////////////////////////////////////////

		public String getKey()
		{
			return key;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance methods : overriding methods
	////////////////////////////////////////////////////////////////////

		@Override
		public String toString()
		{
			return text;
		}

		//--------------------------------------------------------------

	////////////////////////////////////////////////////////////////////
	//  Instance variables
	////////////////////////////////////////////////////////////////////

		private	String	key;
		private	String	text;

	}

	//==================================================================

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public ChunkFilter(Kind kind)
	{
		this.kind = kind;
		ids = new ArrayList<>();
	}

	//------------------------------------------------------------------

	public ChunkFilter(Kind    kind,
					   IffId[] ids)
	{
		this(kind);
		Collections.addAll(this.ids, ids);
	}

	//------------------------------------------------------------------

	public ChunkFilter(Kind        kind,
					   List<IffId> ids)
	{
		this(kind);
		this.ids.addAll(ids);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public ChunkFilter(String str)
	{
		int index = 0;
		if (str.startsWith(Kind.INCLUDE.getKey()))
		{
			kind = Kind.INCLUDE;
			index = Kind.INCLUDE.getKey().length();
		}
		else
		{
			if (str.startsWith(Kind.EXCLUDE.getKey()))
			{
				kind = Kind.EXCLUDE;
				index = Kind.EXCLUDE.getKey().length();
			}
			else
				throw new IllegalArgumentException();
		}

		ids = new ArrayList<>();
		if (str.length() > index)
		{
			char separator = str.charAt(index++);
			String[] strs = str.substring(index).split("\\" + Character.toString(separator));
			char[] spaces = new char[IffId.SIZE - 1];
			Arrays.fill(spaces, ' ');
			String paddingStr = new String(spaces);
			for (String s : strs)
			{
				if (!s.isEmpty())
					ids.add(new IffId(s + paddingStr));
			}
		}
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	private static String stripTrailingSpace(String str)
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

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		return (obj instanceof ChunkFilter other) && (kind == other.kind) && ids.equals(other.ids);
	}

	//------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		return 31 * kind.ordinal() + ids.hashCode();
	}

	//------------------------------------------------------------------

	@Override
	public String toString()
	{
		return (isIncludeAll() || isExcludeAll()) ? getIdString() : kind.getKey() + " " + getIdString();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public Kind getKind()
	{
		return kind;
	}

	//------------------------------------------------------------------

	public int getNumIds()
	{
		return ids.size();
	}

	//------------------------------------------------------------------

	public IffId getId(int index)
	{
		return ids.get(index);
	}

	//------------------------------------------------------------------

	public boolean isInclude()
	{
		return (kind == Kind.INCLUDE);
	}

	//------------------------------------------------------------------

	public boolean isIncludeAll()
	{
		return equals(INCLUDE_ALL);
	}

	//------------------------------------------------------------------

	public boolean isExcludeAll()
	{
		return equals(EXCLUDE_ALL);
	}

	//------------------------------------------------------------------

	public boolean accept(IffId id)
	{
		boolean found = ids.contains(id);
		return (kind == Kind.INCLUDE) ? found : !found;
	}

	//------------------------------------------------------------------

	public String getKeyValue(String separators)
	{
		// Concatenate IDs
		StringBuilder buffer = new StringBuilder(2 + ids.size() * (IffId.SIZE + 1));
		for (IffId id : ids)
			buffer.append(stripTrailingSpace(id.toString()));
		String str = buffer.toString();

		// Search for separators within concatenated ID string
		int index = 0;
		while (index < separators.length())
		{
			if (str.indexOf(separators.charAt(index)) < 0)
				break;
			++index;
		}
		if (index >= separators.length())
			return null;

		// Create key
		char separator = separators.charAt(index);
		buffer.setLength(0);
		buffer.append(kind.getKey());
		for (IffId id : ids)
		{
			buffer.append(separator);
			buffer.append(stripTrailingSpace(id.toString()));
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

	public String getIdString()
	{
		return getIdString(DEFAULT_SEPARATOR);
	}

	//------------------------------------------------------------------

	public String getIdString(char separator)
	{
		if (isIncludeAll())
			return INCLUDE_ALL_STR;

		if (isExcludeAll())
			return EXCLUDE_ALL_STR;

		StringBuilder buffer = new StringBuilder(ids.size() * (IffId.SIZE + 1));
		for (int i = 0; i < ids.size(); i++)
		{
			if (i > 0)
				buffer.append(separator);
			buffer.append(stripTrailingSpace(ids.get(i).toString()));
		}
		return buffer.toString();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	Kind		kind;
	private	List<IffId>	ids;

}

//----------------------------------------------------------------------
