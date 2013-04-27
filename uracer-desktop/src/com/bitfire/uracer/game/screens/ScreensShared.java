
package com.bitfire.uracer.game.screens;

import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;

public class ScreensShared {
	public static String selectedLevelId = "";

	public static void loadFromUserPrefs () {
		String lastTrack = UserPreferences.string(Preference.LastPlayedTrack);
		if (lastTrack.length() > 0) {
			selectedLevelId = lastTrack;
		}
	}

	private ScreensShared () {
	}
}
