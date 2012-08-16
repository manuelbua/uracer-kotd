
package com.bitfire.uracer;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.backends.openal.OpenALAudio;
import com.bitfire.uracer.URacer.URacerFinalizer;

public class URacerDesktopFinalizer implements URacerFinalizer {
	private OpenALAudio audio = null;

	public URacerDesktopFinalizer (OpenALAudio audio) {
		this.audio = audio;
	}

	@Override
	public void dispose () {
		Display.destroy();
		if (this.audio != null) {
			this.audio.dispose();
			this.audio = null;
		}
	}

}
