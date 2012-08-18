
package com.bitfire.uracer.game;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.configuration.Storage;

/** Manages available game tracks. */
public final class GameTracks {

	private static MessageDigest digest;
	private static final Map<String, String> mapHashFilename = new HashMap<String, String>();
	private static final Map<String, String> mapHashName = new HashMap<String, String>();
	private static final Array<String> trackNames = new Array<String>();
	private static final Array<String> trackIds = new Array<String>();

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

		// build internal maps
		for (int i = 0; i < tracks.length; i++) {
			TiledMap m = TiledLoader.createMap(tracks[i]);
			String name = m.properties.get("name");
			if (name != null) {
				String hash = new BigInteger(1, digest.digest(name.getBytes())).toString(16);

				mapHashFilename.put(hash, tracks[i].name());
				mapHashName.put(hash, name);
				trackIds.add(hash);
				trackNames.add(name);

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
				return TiledLoader.createMap(h);
			}
		}

		return null;
	}

	public static Array<String> getAvailableTracks () {
		return trackNames;
	}

	public static Array<String> getAvailableTrackIds () {
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
