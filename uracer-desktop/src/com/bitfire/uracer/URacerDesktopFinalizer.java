
package com.bitfire.uracer;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.bitfire.uracer.URacer.URacerFinalizer;
import com.bitfire.uracer.configuration.BootConfig;

public class URacerDesktopFinalizer implements URacerFinalizer {
	private OpenALAudio audio = null;
	BootConfig boot = null;

	public URacerDesktopFinalizer (BootConfig boot, OpenALAudio audio) {
		this.boot = boot;
		this.audio = audio;
	}

	@Override
	public void dispose () {
		boot.load();
		boot.setWindowX(Display.getX());
		boot.setWindowY(Display.getY());
		boot.store();

		// destroy display
		Display.destroy();

		// destroy audio, if any
		if (this.audio != null) {
			this.audio.dispose();
			this.audio = null;
		}
	}
}
