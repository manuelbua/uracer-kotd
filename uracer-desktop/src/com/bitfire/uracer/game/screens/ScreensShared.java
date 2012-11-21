
package com.bitfire.uracer.game.screens;

import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;

public class ScreensShared {
	public static String selectedTrackId = "";

	public static void loadFromUserPrefs () {
		String lastTrack = UserPreferences.string(Preference.LastPlayedTrack);
		if (lastTrack.length() > 0) {
			selectedTrackId = lastTrack;
		}
	}

	private ScreensShared () {
	}
}
