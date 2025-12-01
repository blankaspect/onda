/*====================================================================*\

IffGroup.java

IFF group class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

//----------------------------------------------------------------------


// IFF GROUP CLASS


public class IffGroup
	extends Group
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public IffGroup()
	{
	}

	//------------------------------------------------------------------

	public IffGroup(IffId groupId,
					IffId typeId)
	{
		super(groupId, typeId);
	}

	//------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException
	 */

	public IffGroup(byte[] headerBytes)
	{
		super(headerBytes);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int putHeader(IffId  groupId,
								IffId  typeId,
								int    size,
								byte[] buffer)
	{
		groupId.put(buffer);
		IffChunk.putSize(size, buffer, IffId.SIZE);
		typeId.put(buffer, Chunk.HEADER_SIZE);
		return Group.HEADER_SIZE;
	}

	//------------------------------------------------------------------

	public static int putHeader(IffId  groupId,
								IffId  typeId,
								int    size,
								byte[] buffer,
								int    offset)
	{
		groupId.put(buffer, offset);
		IffChunk.putSize(size, buffer, offset + IffId.SIZE);
		typeId.put(buffer, offset + Chunk.HEADER_SIZE);
		return offset + Group.HEADER_SIZE;
	}

	//------------------------------------------------------------------

	public static void writeHeader(OutputStream outStream,
								   IffId        groupId,
								   IffId        typeId,
								   int          size)
		throws IOException
	{
		byte[] buffer = new byte[Group.HEADER_SIZE];
		putHeader(groupId, typeId, size, buffer);
		outStream.write(buffer);
	}

	//------------------------------------------------------------------

	public static void writeHeader(DataOutput dataOutput,
								   IffId      groupId,
								   IffId      typeId,
								   int        size)
		throws IOException
	{
		byte[] buffer = new byte[Group.HEADER_SIZE];
		putHeader(groupId, typeId, size, buffer);
		dataOutput.write(buffer);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public int putHeader(byte[] buffer)
	{
		return putHeader(groupId, typeId, getSize(), buffer);
	}

	//------------------------------------------------------------------

	public int putHeader(byte[] buffer,
						 int    offset)
	{
		return putHeader(groupId, typeId, getSize(), buffer, offset);
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
