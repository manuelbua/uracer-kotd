
package com.bitfire.uracer.configuration;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.bitfire.postprocessing.filters.RadialBlur;

/** Represents user-configurable configuration properties.
 * 
 * The functionalities can be safely accessed statically just after user preferences have been loaded.
 * 
 * Keep in mind that querying configuration values is time consuming so use it with care and use for initialization or one-off
 * stuff. If you really need to do that per-frame then think about caching the values and just refresh them once every n-frames.
 * 
 * On the desktop, user settings are stored in ~/.prefs/ while on mobile we are wrapping SharedPreferences. */
public final class UserPreferences {

	public enum Preference {
		// @off

		// post-processing
		PostProcessing, Vignetting, Bloom, ZoomRadialBlur, ZoomRadialBlurQuality, CrtScreen, Curvature,

		// rendering
		ComplexTrees, Walls,

		// gameplay
		TimeDilateInputMode, ;
		// @on

		public String name;

		private Preference () {
			this.name = this.name();
		}

		private Preference (String name) {
			this.name = name;
		}
	}

	public static void load () {
		prefs = Gdx.app.getPreferences(Config.UserPreferences);

		if (prefs.get().size() == 0) {
			toDefault();
		}
	}

	public static void save () {
		prefs.flush();
		Gdx.app.debug("UserPreferences", "User preferences updated.");
	}

	private static void toDefault () {
		prefs.clear();

		//
		// post-processing
		//

		bool(Preference.PostProcessing, true);
		bool(Preference.Vignetting, Config.isDesktop);
		bool(Preference.Bloom, false);
		bool(Preference.ZoomRadialBlur, true);

		//
		// rendering
		//

		bool(Preference.ComplexTrees, false);

		// detect default radial blur quality inspecting
		// the current game resolution
		int w = Gdx.graphics.getWidth();
		if (w >= 1680) {
			string(Preference.ZoomRadialBlurQuality, RadialBlur.Quality.High.toString());
		} else if (w >= 1200) {
			string(Preference.ZoomRadialBlurQuality, RadialBlur.Quality.Medium.toString());
		} else if (w >= 800) {
			string(Preference.ZoomRadialBlurQuality, RadialBlur.Quality.Low.toString());
		}

		bool(Preference.CrtScreen, false);
		bool(Preference.Curvature, false);

		//
		// gameplay
		//

		string(Preference.TimeDilateInputMode, Gameplay.TimeDilateInputMode.TouchAndRelease.toString());

		// ensure the new configuration gets saved
		prefs.flush();
	}

	/** boolean */
	public static boolean bool (Preference pref) {
		return prefs.getBoolean(pref.name);
	}

	public static void bool (Preference pref, boolean value) {
		prefs.putBoolean(pref.name, value);
	}

	/** get integer */
	public static int integer (Preference pref) {
		return prefs.getInteger(pref.name);
	}

	public static void integer (Preference pref, int value) {
		prefs.putInteger(pref.name, value);
	}

	/** get long */
	public static long longint (Preference pref) {
		return prefs.getLong(pref.name);
	}

	public static void longint (Preference pref, long value) {
		prefs.putLong(pref.name, value);
	}

	/** get float */
	public static float real (Preference pref) {
		return prefs.getFloat(pref.name);
	}

	public static void real (Preference pref, float value) {
		prefs.putFloat(pref.name, value);
	}

	/** get string */
	public static String string (Preference pref) {
		return prefs.getString(pref.name);
	}

	public static void string (Preference pref, String value) {
		prefs.putString(pref.name, value);
	}

	private static Preferences prefs = null;

	private UserPreferences () {
	}
}
