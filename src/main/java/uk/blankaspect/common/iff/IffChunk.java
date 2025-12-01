/*====================================================================*\

IffChunk.java

IFF chunk class.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import uk.blankaspect.common.number.NumberCodec;

//----------------------------------------------------------------------


// IFF CHUNK CLASS


public class IffChunk
	extends Chunk
{

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public IffChunk()
	{
	}

	//------------------------------------------------------------------

	public IffChunk(byte[] bytes)
	{
		set(bytes);
	}

	//------------------------------------------------------------------

	public IffChunk(IffId  id,
					byte[] data)
	{
		super(id, data);
	}

	//------------------------------------------------------------------

	public IffChunk(IffId  id,
					byte[] data,
					int    offset,
					int    length)
	{
		super(id, data, offset, length);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static int getSize(byte[] bytes)
	{
		return getSize(bytes, 0);
	}

	//------------------------------------------------------------------

	public static int getSize(byte[] bytes,
							  int    offset)
	{
		return NumberCodec.bytesToUIntBE(bytes, offset, SIZE_SIZE);
	}

	//------------------------------------------------------------------

	public static void putSize(int    size,
							   byte[] buffer)
	{
		putSize(size, buffer, 0);
	}

	//------------------------------------------------------------------

	public static void putSize(int    size,
							   byte[] buffer,
							   int    offset)
	{
		NumberCodec.uIntToBytesBE(size, buffer, offset, SIZE_SIZE);
	}

	//------------------------------------------------------------------

	public static int putHeader(IffId  id,
								int    size,
								byte[] buffer)
	{
		id.put(buffer);
		putSize(size, buffer, IffId.SIZE);
		return Chunk.HEADER_SIZE;
	}

	//------------------------------------------------------------------

	public static int putHeader(IffId  id,
								int    size,
								byte[] buffer,
								int    offset)
	{
		id.put(buffer, offset);
		putSize(size, buffer, offset + IffId.SIZE);
		return offset + Chunk.HEADER_SIZE;
	}

	//------------------------------------------------------------------

	public static void writeHeader(OutputStream outStream,
								   IffId        id,
								   int          size)
		throws IOException
	{
		byte[] buffer = new byte[Chunk.HEADER_SIZE];
		putHeader(id, size, buffer);
		outStream.write(buffer);
	}

	//------------------------------------------------------------------

	public static void writeHeader(DataOutput dataOutput,
								   IffId      id,
								   int        size)
		throws IOException
	{
		byte[] buffer = new byte[Chunk.HEADER_SIZE];
		putHeader(id, size, buffer);
		dataOutput.write(buffer);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public void putHeader(byte[] buffer)
	{
		putHeader(buffer, 0);
	}

	//------------------------------------------------------------------

	public void putHeader(byte[] buffer,
						  int    offset)
	{
		putHeader(id, getSize(), buffer, offset);
	}

	//------------------------------------------------------------------

	public void put(byte[] buffer)
	{
		put(buffer, 0);
	}

	//------------------------------------------------------------------

	public void put(byte[] buffer,
					int    offset)
	{
		putHeader(buffer, offset);
		if (data != null)
		{
			int size = data.length;
			System.arraycopy(data, 0, buffer, offset + Chunk.HEADER_SIZE, size);
			if ((size & 1) != 0)
				buffer[offset + Chunk.HEADER_SIZE + size] = 0;
		}
	}

	//------------------------------------------------------------------

	public void set(byte[] chunkBytes)
	{
		super.set(new IffId(chunkBytes), chunkBytes, Chunk.HEADER_SIZE, getSize(chunkBytes, IffId.SIZE));
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
