/*====================================================================*\

Chunk.java

Class: abstract IFF chunk.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.common.iff;

//----------------------------------------------------------------------


// IMPORTS


import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

//----------------------------------------------------------------------


// CLASS: ABSTRACT IFF CHUNK


public abstract class Chunk
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	public static final	int	SIZE_SIZE	= 4;
	public static final	int	HEADER_SIZE	= IffId.SIZE + SIZE_SIZE;

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	protected	IffId	id;
	protected	byte[]	data;

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	public Chunk()
	{
		data = new byte[0];
	}

	//------------------------------------------------------------------

	public Chunk(IffId  id,
				 byte[] data)
	{
		set(id, data);
	}

	//------------------------------------------------------------------

	public Chunk(IffId  id,
				 byte[] data,
				 int    offset,
				 int    length)
	{
		set(id, data, offset, length);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	protected abstract void putHeader(byte[] buffer);

	//------------------------------------------------------------------

	protected abstract void putHeader(byte[] buffer,
									  int    offset);

	//------------------------------------------------------------------

	protected abstract void put(byte[] buffer);

	//------------------------------------------------------------------

	protected abstract void put(byte[] buffer,
								int    offset);

	//------------------------------------------------------------------

	protected abstract void set(byte[] bytes);

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public IffId getId()
	{
		return id;
	}

	//------------------------------------------------------------------

	public int getSize()
	{
		return ((data == null) ? 0 : data.length);
	}

	//------------------------------------------------------------------

	public byte[] getData()
	{
		return data;
	}

	//------------------------------------------------------------------

	public int getExtent()
	{
		int offset = HEADER_SIZE + getSize();
		if ((offset & 1) != 0)
			++offset;
		return offset;
	}

	//------------------------------------------------------------------

	public void setId(byte[] bytes)
	{
		id.set(bytes);
	}

	//------------------------------------------------------------------

	public void setId(byte[] bytes,
					  int    offset)
	{
		id.set(bytes, offset);
	}

	//------------------------------------------------------------------

	public void setId(String str)
	{
		id.set(str);
	}

	//------------------------------------------------------------------

	public void setId(IffId id)
	{
		this.id = id;
	}

	//------------------------------------------------------------------

	public void set(IffId  id,
					byte[] data)
	{
		this.id = id;
		this.data = data;
	}

	//------------------------------------------------------------------

	public void set(IffId  id,
					byte[] data,
					int    offset,
					int    length)
	{
		this.id = id;
		this.data = new byte[length];
		System.arraycopy(data, offset, this.data, 0, length);
	}

	//------------------------------------------------------------------

	public void write(OutputStream outStream)
		throws IOException
	{
		byte header[] = new byte[HEADER_SIZE];
		putHeader(header);
		outStream.write(header);
		if (data != null)
		{
			outStream.write(data);
			if ((data.length & 1) != 0)
				outStream.write(0);
		}
	}

	//------------------------------------------------------------------

	public void write(DataOutput dataOutput)
		throws IOException
	{
		byte header[] = new byte[HEADER_SIZE];
		putHeader(header);
		dataOutput.write(header);
		if (data != null)
		{
			dataOutput.write(data);
			if ((data.length & 1) != 0)
				dataOutput.write(0);
		}
	}

	//------------------------------------------------------------------

}

//----------------------------------------------------------------------
