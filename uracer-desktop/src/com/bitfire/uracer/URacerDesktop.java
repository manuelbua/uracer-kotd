
package com.bitfire.uracer;

import java.util.Calendar;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.bitfire.uracer.configuration.BootConfig;
import com.bitfire.uracer.configuration.BootConfig.BootConfigFlag;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.utils.CommandLine;

public final class URacerDesktop {

	// private static boolean useRightScreen = false;

	private static LwjglApplicationConfiguration createLwjglConfig (BootConfig boot) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		// set to uracer defaults
		config.addIcon("data/base/icon.png", FileType.Internal);
		config.title = URacer.Name + " (" + URacer.versionInfo + ")";
		config.resizable = false;
		config.samples = 0;
		config.audioDeviceSimultaneousSources = 32;

		if (Config.Debug.PauseDisabled) {
			config.backgroundFPS = 0;
			config.foregroundFPS = 0;
		} else {
			config.backgroundFPS = 60;
			config.foregroundFPS = 60;
		}

		// apply boot config
		config.width = boot.getInt(BootConfigFlag.WIDTH);
		config.height = boot.getInt(BootConfigFlag.HEIGHT);
		config.vSyncEnabled = boot.getBoolean(BootConfigFlag.VSYNC);
		config.fullscreen = boot.getBoolean(BootConfigFlag.FULLSCREEN);

		return config;
	}

	public static void main (String[] argv) {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		System.out.print(URacer.Name + " (" + URacer.versionInfo + ")\nCopyright (c) 2011-" + year + " Manuel Bua.\n\n");

		// load boot configuration, either from file or from defaults
		BootConfig boot = new BootConfig();

		// override boot config by command line flags, if any
		if (argv.length > 0) {
			if (!CommandLine.applyLaunchFlags(argv, boot)) {
				return;
			}
		} else {
			System.out.println("Try --help for a list of valid command-line switches.\n");
		}

		System.setProperty("org.lwjgl.opengl.Window.undecorated", "" + boot.getBoolean(BootConfigFlag.UNDECORATED));

		LwjglApplicationConfiguration config = createLwjglConfig(boot);

		System.out.print("Resolution set at " + (config.width + "x" + config.height) + " (x=" + boot.getWindowX() + ", y="
			+ boot.getWindowY() + ")\n");
		System.out.print("Vertical sync: " + (config.vSyncEnabled ? "Yes" : "No") + "\n");
		System.out.print("Fullscreen: " + (config.fullscreen ? "Yes" : "No") + "\n");
		System.out.print("Window decorations: " + (boot.getBoolean(BootConfigFlag.UNDECORATED) ? "No" : "Yes") + "\n");

		URacer uracer = new URacer(boot);
		LwjglApplication app = new LwjglApplication(uracer, config);

		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer(boot, (OpenALAudio)app.getAudio());
		uracer.setFinalizer(finalizr);

		if (!config.fullscreen) {
			Display.setLocation(boot.getWindowX(), boot.getWindowY());
		}
	}

	private URacerDesktop () {
	}
}
