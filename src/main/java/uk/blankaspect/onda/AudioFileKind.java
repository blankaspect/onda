/*====================================================================*\

AudioFileKind.java

Audio file enumeration.

\*====================================================================*/


// PACKAGE


package uk.blankaspect.onda;

//----------------------------------------------------------------------


// IMPORTS


import java.io.File;

import uk.blankaspect.common.audio.AiffFile;
import uk.blankaspect.common.audio.AudioFile;
import uk.blankaspect.common.audio.WaveFile;

import uk.blankaspect.common.exception.AppException;

import uk.blankaspect.common.iff.Chunk;
import uk.blankaspect.common.iff.IffChunk;
import uk.blankaspect.common.iff.IffId;
import uk.blankaspect.common.iff.RiffChunk;

import uk.blankaspect.common.misc.IStringKeyed;

//----------------------------------------------------------------------


// AUDIO FILE ENUMERATION


enum AudioFileKind
	implements IStringKeyed
{

////////////////////////////////////////////////////////////////////////
//  Constants
////////////////////////////////////////////////////////////////////////

	AIFF
	(
		AudioFile.Kind.AIFF,
		CriticalIds.AIFF
	)
	{
		@Override
		public Chunk createChunk()
		{
			return new IffChunk();
		}
	},

	WAVE
	(
		AudioFile.Kind.WAVE,
		CriticalIds.WAVE
	)
	{
		@Override
		public Chunk createChunk()
		{
			return new RiffChunk();
		}
	};

	//------------------------------------------------------------------

	private interface CriticalIds
	{
		IffId[]	AIFF	=
		{
			AiffFile.AIFF_COMMON_ID,
			AiffFile.AIFF_DATA_ID
		};
		IffId[]	WAVE	=
		{
			WaveFile.WAVE_FORMAT_ID,
			WaveFile.WAVE_DATA_ID
		};
	}

////////////////////////////////////////////////////////////////////////
//  Constructors
////////////////////////////////////////////////////////////////////////

	private AudioFileKind(AudioFile.Kind fileKind,
						  IffId[]        criticalIds)
	{
		this.fileKind = fileKind;
		this.criticalIds = criticalIds;
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Class methods
////////////////////////////////////////////////////////////////////////

	public static AudioFileKind forKey(String key)
	{
		return forFileKind(AudioFile.Kind.forKey(key));
	}

	//------------------------------------------------------------------

	public static AudioFileKind forFileKind(AudioFile.Kind fileKind)
	{
		for (AudioFileKind value : values())
		{
			if (value.fileKind == fileKind)
				return value;
		}
		return null;
	}

	//------------------------------------------------------------------

	public static AudioFileKind forFile(File file)
		throws AppException
	{
		return forFileKind(AudioFile.Kind.forFile(file));
	}

	//------------------------------------------------------------------

	public static AudioFileKind forFilename(String filename)
	{
		return forFileKind(AudioFile.Kind.forFilename(filename));
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Abstract methods
////////////////////////////////////////////////////////////////////////

	public abstract Chunk createChunk();

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : IStringKeyed interface
////////////////////////////////////////////////////////////////////////

	public String getKey()
	{
		return fileKind.getKey();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods : overriding methods
////////////////////////////////////////////////////////////////////////

	@Override
	public String toString()
	{
		return fileKind.toString();
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance methods
////////////////////////////////////////////////////////////////////////

	public IffId[] getCriticalIds()
	{
		return criticalIds;
	}

	//------------------------------------------------------------------

	public AudioFile createFile(File file)
	{
		return fileKind.createFile(file);
	}

	//------------------------------------------------------------------

	public AudioFile createFile(File file,
								int  numChannels,
								int  bitsPerSample,
								int  sampleRate)
	{
		return fileKind.createFile(file, numChannels, bitsPerSample, sampleRate);
	}

	//------------------------------------------------------------------

////////////////////////////////////////////////////////////////////////
//  Instance variables
////////////////////////////////////////////////////////////////////////

	private	AudioFile.Kind	fileKind;
	private	IffId[]			criticalIds;

}

//----------------------------------------------------------------------
