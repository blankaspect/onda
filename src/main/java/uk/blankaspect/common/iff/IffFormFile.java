/*====================================================================*\

IffFormFile.java

IFF form file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

//----------------------------------------------------------------------


// IFF FORM FILE CLASS


public class IffFormFile
	extends FormFile
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	IffId	GROUP_ID	= new IffId("FORM");

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public IffFormFile(File file)
	{
		super(GROUP_ID, file);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	protected int getChunkSize(byte[] sizeBytes,
							   int    offset)
	{
		return IffChunk.getSize(sizeBytes, offset);
	}

	//------------------------------------------------------------------

	@Override
	protected void writeGroupHeader(DataOutput dataOutput,
									IffId      groupId,
									IffId      typeId,
									int        size)
		throws IOException
	{
		IffGroup.writeHeader(dataOutput, groupId, typeId, size);
	}

	//------------------------------------------------------------------

	@Override
	protected void writeChunkHeader(DataOutput dataOutput,
									IffId      id,
									int        size)
		throws IOException
	{
		IffChunk.writeHeader(dataOutput, id, size);
	}

	//------------------------------------------------------------------

	@Override
	protected void putSize(int    size,
						   byte[] buffer)
	{
		IffChunk.putSize(size, buffer);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	@Override
	protected Group createGroup(byte[] header)
	{
		return new IffGroup(header);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
