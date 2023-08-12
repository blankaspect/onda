/*====================================================================*\

IffException.java

Class: IFF exception.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import uk.blankaspect.common.exception.AppException;
import uk.blankaspect.common.exception.ExceptionUtils;

//----------------------------------------------------------------------


// CLASS: IFF EXCEPTION


public class IffException
	extends AppException
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	int	MAX_PATHNAME_LENGTH	= 160;

	private static final	String	CHUNK_ID_STR	= "Chunk ID";

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	File	file;
	private	IffId	chunkId;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public IffException(AppException.IId id,
						File             file,
						IffId            chunkId)
	{
		super(id);
		this.file = file;
		this.chunkId = chunkId;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	protected static String getPathname(File file)
	{
		return ExceptionUtils.getLimitedPathname(file, MAX_PATHNAME_LENGTH);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	protected String getPrefix()
	{
		return (getPathname(file) + "\n" + CHUNK_ID_STR + ": '" + chunkId + "'");
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
