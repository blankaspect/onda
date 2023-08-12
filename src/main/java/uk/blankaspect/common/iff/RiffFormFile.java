/*====================================================================*\

RiffFormFile.java

RIFF form file class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

//----------------------------------------------------------------------


// RIFF FORM FILE CLASS


public class RiffFormFile
	extends FormFile
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	private static final	IffId	GROUP_ID	= new IffId("RIFF");

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public RiffFormFile(File file)
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
		return RiffChunk.getSize(sizeBytes, offset);
	}

	//------------------------------------------------------------------

	@Override
	protected void writeGroupHeader(DataOutput dataOutput,
									IffId      groupId,
									IffId      typeId,
									int        size)
		throws IOException
	{
		RiffGroup.writeHeader(dataOutput, groupId, typeId, size);
	}

	//------------------------------------------------------------------

	@Override
	protected void writeChunkHeader(DataOutput dataOutput,
									IffId      id,
									int        size)
		throws IOException
	{
		RiffChunk.writeHeader(dataOutput, id, size);
	}

	//------------------------------------------------------------------

	@Override
	protected void putSize(int    size,
						   byte[] buffer)
	{
		RiffChunk.putSize(size, buffer);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	@Override
	protected Group createGroup(byte[] header)
	{
		return new RiffGroup(header);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
