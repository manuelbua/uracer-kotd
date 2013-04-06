
package com.bitfire.uracer;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.openal.OpenALAudio;
import com.bitfire.uracer.configuration.LaunchFlags;
import com.bitfire.uracer.utils.CommandLine;

public final class URacerDesktop {

	// private static boolean useRightScreen = false;

	private static boolean parseConfig (String[] argv, LwjglApplicationConfiguration config) {

		LaunchFlags flags = new LaunchFlags();
		if (!CommandLine.parseLaunchFlags(argv, flags)) {
			return false;
		}

		// set to default
		config.title = URacer.Name + " (" + URacer.versionInfo + ")";
		config.useGL20 = true;
		config.resizable = false;

		config.width = flags.width;
		config.height = flags.height;
		config.vSyncEnabled = flags.vSyncEnabled;
		config.useCPUSynch = flags.useCPUSynch;
		config.fullscreen = flags.fullscreen;
		// useRightScreen = flags.useRightScreen;

		// parse opts --

		System.out.print("Running on desktop\n");
		System.out.print("Resolution set at " + (config.width + "x" + config.height) + "\n");
		System.out.print("Vertical sync: " + (config.vSyncEnabled ? "On" : "Off") + "\n");
		System.out.print("CPU sync: " + (config.useCPUSynch ? "On" : "Off") + "\n");
		System.out.print("Fullscreen: " + (config.fullscreen ? "Yes" : "No") + "\n");

		return true;
	}

	private static String bootConfigFile = "uracer-boot.cfg";
	private static Properties bootConfig = new Properties();

	private static void boot () {
		System.out.print(URacer.Name + "\nCopyright (c) 2012-2013 Manuel Bua.\n\n");

		try {
			bootConfig.load(new FileInputStream(bootConfigFile));
		} catch (IOException e) {
			System.out.print("NO BOOT CONFIG AVAILABLE\n\n");
		}
	}

	private static int bootConfigI (String name, int value) {
		String v = bootConfig.getProperty(name);
		if (v == null) {
			return value;
		}
		return Integer.parseInt(v);
	}

	public static void main (String[] argv) {
		boot();

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.addIcon("data/base/icon.png", FileType.Internal);

		if (!parseConfig(argv, config)) {
			return;
		}

		URacer uracer = new URacer();
		LwjglApplication app = new LwjglApplication(uracer, config);

		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer(bootConfigFile, (OpenALAudio)app.getAudio());
		uracer.setFinalizer(finalizr);

		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice primary = env.getDefaultScreenDevice();

		if (primary != null) {
			java.awt.DisplayMode mode = primary.getDisplayMode();

			int xoffset = 0;
			int yoffset = 0;
			int x = xoffset + (mode.getWidth() - config.width) / 2;
			int y = yoffset + (mode.getHeight() - config.height) / 2;

			int nx = bootConfigI("win." + mode.getWidth() + "x" + mode.getHeight() + ".x", x);
			int ny = bootConfigI("win." + mode.getWidth() + "x" + mode.getHeight() + ".y", y);
			Display.setLocation(nx, ny);
		}
	}

	private URacerDesktop () {
	}
}
