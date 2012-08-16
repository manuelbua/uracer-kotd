
package com.bitfire.uracer;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.openal.OpenALAudio;
import com.bitfire.uracer.configuration.LaunchFlags;
import com.bitfire.uracer.utils.CommandLine;

public final class URacerDesktop {

	private static boolean useRightScreen = false;

	private static boolean parseConfig (String[] argv, LwjglApplicationConfiguration config) {

		System.out.print(URacer.Name + " " + URacer.getVersionInformation() + "\nCopyright (c) 2012 Manuel Bua.\n\n");

		LaunchFlags flags = new LaunchFlags();
		if (!CommandLine.parseLaunchFlags(argv, flags)) {
			return false;
		}

		// set to default
		config.title = URacer.Name;
		config.useGL20 = true;
		config.resizable = false;

		config.width = flags.width;
		config.height = flags.height;
		config.vSyncEnabled = flags.vSyncEnabled;
		config.useCPUSynch = flags.useCPUSynch;
		config.fullscreen = flags.fullscreen;
		useRightScreen = flags.useRightScreen;

		// parse opts --

		System.out.print("Resolution set at " + (config.width + "x" + config.height) + "\n");
		System.out.print("Vertical sync: " + (config.vSyncEnabled ? "On" : "Off") + "\n");
		System.out.print("CPU sync: " + (config.useCPUSynch ? "On" : "Off") + "\n");
		System.out.print("Fullscreen: " + (config.fullscreen ? "Yes" : "No") + "\n");

		return true;
	}

	public static void main (String[] argv) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.addIcon("data/base/icon.png", FileType.Internal);

		if (!parseConfig(argv, config)) {
			return;
		}

		URacer uracer = new URacer();
		LwjglApplication app = new LwjglApplication(uracer, config);

		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer((OpenALAudio)app.getAudio());
		uracer.setFinalizer(finalizr);

		if (useRightScreen) {
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice primary = env.getDefaultScreenDevice();
			GraphicsDevice[] devices = env.getScreenDevices();
			GraphicsDevice target = null;

			// search for the first target screen
			for (int i = 0; i < devices.length; i++) {
				boolean isPrimary = (primary == devices[i]);
				if (!isPrimary) {
					target = devices[i];
					break;
				}
			}

			if (target != null) {
				DisplayMode pmode = primary.getDisplayMode();
				DisplayMode tmode = target.getDisplayMode();

				Display
					.setLocation(pmode.getWidth() + (tmode.getWidth() - config.width) / 2, (tmode.getHeight() - config.height) / 2);
			}
		}
	}

	private URacerDesktop () {
	}
}
