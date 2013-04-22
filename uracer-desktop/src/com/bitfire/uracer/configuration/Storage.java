
package com.bitfire.uracer.configuration;

import com.badlogic.gdx.Gdx;

/** Represents storage path specifiers, such as levels and replays data store, */
public final class Storage {

	public static final String URacerHome = "uracer";
	public static final String URacerHomePath = System.getProperty("user.home") + "/" + URacerHome + "/";
	public static final String BootConfigFile = URacerHomePath + "uracer-boot.cfg";

	public static final String Levels = "data/levels/";
	public static final String UI = "data/ui/";
	public static final String LocalReplays = URacerHome + "/local-replays/";

	public static void createDirs () {
		// create URacer home path
		if (!Gdx.files.external(Storage.URacerHome).exists()) {
			Gdx.files.external(Storage.URacerHome).mkdirs();
		}

		if (!Gdx.files.external(Storage.LocalReplays).exists()) {
			Gdx.files.external(Storage.LocalReplays).mkdirs();
		}
	}

	private Storage () {
	}
}
