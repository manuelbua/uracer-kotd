
package com.bitfire.uracer.configuration;

import com.badlogic.gdx.Gdx;

/** Represents storage path specifiers, such as levels and replays data store, */
public final class Storage {

	// externals
	private static final String ConfigRoot = "/.config/uracer/";
	private static final String DataRoot = "/.local/share/uracer/";
	public static final String ReplaysRoot = DataRoot + "local-replays/";
	public static final String Preferences = "uracer-preferences.cfg";

	// local to installation folder
	public static final String BootConfigFile = "uracer-boot.cfg";
	public static final String Levels = "data/levels/";
	public static final String UI = "data/ui/";

	public static void init () {
		// // determine user directories on UNIX-like OSes
		// String configBase = System.getenv("XDG_CONFIG_HOME");
		// if (configBase == null || configBase.trim().equals("")) {
		// configBase = System.getProperty("user.home") + "/" + ".config";
		// }

		Gdx.files.external(Storage.ConfigRoot).mkdirs();
		Gdx.files.external(Storage.DataRoot).mkdirs();
		Gdx.files.external(Storage.ReplaysRoot).mkdirs();
	}

	private Storage () {
	}
}
