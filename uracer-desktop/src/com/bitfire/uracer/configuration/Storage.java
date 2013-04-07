
package com.bitfire.uracer.configuration;

/** Represents storage path specifiers, such as levels and replays data store, */
public final class Storage {

	public static final String URacerHome = "uracer";
	public static final String BootConfigFile = System.getProperty("user.home") + "/" + URacerHome + "/uracer-boot.cfg";

	public static final String Levels = "data/levels/";
	public static final String UI = "data/ui/";
	public static final String LocalReplays = URacerHome + "/local-replays/";

	private Storage () {
	}
}
