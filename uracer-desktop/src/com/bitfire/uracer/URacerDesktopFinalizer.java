
package com.bitfire.uracer;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.backends.openal.OpenALAudio;
import com.bitfire.uracer.URacer.URacerFinalizer;

public class URacerDesktopFinalizer implements URacerFinalizer {
	private OpenALAudio audio = null;
	private String bootConfigFile = "";

	public URacerDesktopFinalizer (String bootConfigFile, OpenALAudio audio) {
		this.audio = audio;
		this.bootConfigFile = bootConfigFile;
	}

	@Override
	public void dispose () {
		Properties bootConfig = new Properties();

		// save window position
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice primary = env.getDefaultScreenDevice();
		if (primary != null) {
			java.awt.DisplayMode mode = primary.getDisplayMode();
			bootConfig.setProperty("win." + mode.getWidth() + "x" + mode.getHeight() + ".x", "" + Display.getX());
			bootConfig.setProperty("win." + mode.getWidth() + "x" + mode.getHeight() + ".y", "" + Display.getY());
		}

		try {
			bootConfig.store(new FileOutputStream(bootConfigFile), null);
		} catch (IOException e) {
			System.out.print("CANNOT STORE BOOT CONFIG\n");
		}

		// destroy display
		Display.destroy();

		// destroy audio, if any
		if (this.audio != null) {
			this.audio.dispose();
			this.audio = null;
		}
	}
}
