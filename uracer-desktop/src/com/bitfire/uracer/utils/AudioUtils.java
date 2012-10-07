
package com.bitfire.uracer.utils;

public final class AudioUtils {

	private AudioUtils () {
	}

	public static float timeDilationToAudioPitch (float pitchIn, float timeMultiplier) {
		return pitchIn - (1 - timeMultiplier) * 0.4f;
	}

}
