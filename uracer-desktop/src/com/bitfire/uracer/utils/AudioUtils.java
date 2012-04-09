package com.bitfire.uracer.utils;

public class AudioUtils {

	public static final float timeDilationToAudioPitch( float pitchIn, float timeMultiplier ) {
		return pitchIn - (1 - timeMultiplier) * 0.3f;
	}

}
