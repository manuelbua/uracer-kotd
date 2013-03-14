
package com.bitfire.uracer.game;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.game.world.URacerTmxMapLoader;

/** Enumerates and maintains a list of available game tracks. FIXME add support for mini-screenshots */
public final class GameTracks {

	private static MessageDigest digest;
	private static final Map<String, String> mapHashFilename = new HashMap<String, String>();
	private static final Map<String, String> mapHashName = new HashMap<String, String>();
	private static String[] trackNames;
	private static String[] trackIds;
	private static TmxMapLoader mapLoader = new URacerTmxMapLoader();

// private static TmxMapLoader mapLoader = new TmxMapLoader();
	private static final boolean yUp = false;

	public static final boolean init () {

		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			Gdx.app.log("URacer", "Support for SHA-1 crypto not found.");
			return false;
		}

		// enumerate available tracks

		// invalid path?
		FileHandle dirLevels = Gdx.files.internal(Storage.Levels);
		if (dirLevels == null || !dirLevels.isDirectory()) {
			Gdx.app.log("URacer", "Cannot check for available game tracks.");
			return false;
		}

		FileHandle[] tracks = dirLevels.list("tmx");
		if (tracks.length == 0) {
			Gdx.app.log("URacer", "No available game tracks found.");
			return false;
		}

		trackNames = new String[tracks.length];
		trackIds = new String[tracks.length];

		// build internal maps
		for (int i = 0; i < tracks.length; i++) {
			TiledMap m = mapLoader.load(tracks[i].path(), yUp);
			String name = m.getProperties().get("name", String.class);
			if (name != null) {
				String hash = new BigInteger(1, digest.digest(name.getBytes())).toString(16);

				mapHashFilename.put(hash, tracks[i].name());
				mapHashName.put(hash, name);
				trackIds[i] = hash;
				trackNames[i] = name;

				Gdx.app.log("GameTracks", "Found track \"" + name + "\" (" + hash + ")");
			} else {
				Gdx.app.log("GameTracks", "Track \"" + tracks[i].name() + "\" ignored, bad name descriptor");
			}
		}

		return true;
	}

	public static TiledMap load (String trackId) {
		String filename = mapHashFilename.get(trackId);
		if (filename != null) {
			FileHandle h = Gdx.files.internal(Storage.Levels + filename);
			if (h.exists()) {
				return mapLoader.load(h.path(), yUp);
			}
		}

		return null;
	}

	public static String[] getAvailableTracks () {
		return trackNames;
	}

	public static String[] getAvailableTrackIds () {
		return trackIds;
	}

	public static String getTrackId (String trackName) {
		for (Map.Entry<String, String> e : mapHashName.entrySet()) {
			if (trackName.equals(e.getValue())) {
				return e.getKey();
			}
		}

		return null;
	}

	private GameTracks () {
	}
}
