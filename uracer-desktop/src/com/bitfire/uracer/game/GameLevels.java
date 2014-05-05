
package com.bitfire.uracer.game;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.utils.DigestUtils;
import com.bitfire.uracer.utils.UAtlasTmxMapLoader;
import com.bitfire.uracer.utils.URacerRuntimeException;

/** Enumerates and maintains a list of available game tracks. FIXME add support for mini-screenshots
 * 
 * uRacer map levels are assumed to be encoded as base64+zlib compression. The checksummed layer shall be named "track". */
public final class GameLevels {

	private static final Map<String, GameLevelDescriptor> levelIdToDescriptor = new HashMap<String, GameLevelDescriptor>();
	private static final UAtlasTmxMapLoader mapLoader = new UAtlasTmxMapLoader();
	private static final UAtlasTmxMapLoader.AtlasTiledMapLoaderParameters mapLoaderParams = new UAtlasTmxMapLoader.AtlasTiledMapLoaderParameters();
	private static final XmlReader xml = new XmlReader();
	private static final List<GameLevelDescriptor> levels = new ArrayList<GameLevels.GameLevelDescriptor>();

	// cached simple array[] return type
	private static GameLevelDescriptor[] levelsCache = null;

	public static class GameLevelDescriptor implements Comparable<GameLevelDescriptor> {
		private String name;
		private BigInteger checksum;
		private String filename;

		public GameLevelDescriptor (String name, BigInteger checksum, String filename) {
			this.name = name;
			this.checksum = checksum;
			this.filename = filename;
		}

		public String getId () {
			return checksum.toString(16);
		}

		public BigInteger getChecksum () {
			return checksum;
		}

		public String getName () {
			return name;
		}

		public String getFileName () {
			return filename;
		}

		@Override
		public int compareTo (GameLevelDescriptor o) {
			return name.compareTo(o.getName());
		}

		@Override
		public String toString () {
			return name;
		}
	}

	public static final boolean init () {

		// setup map loader
		mapLoaderParams.forceTextureFilters = true;
		mapLoaderParams.textureMinFilter = TextureFilter.Linear;
		mapLoaderParams.textureMagFilter = TextureFilter.Linear;
		mapLoaderParams.yUp = false;

		// check invalid
		FileHandle dirLevels = Gdx.files.internal(Storage.Levels);
		if (dirLevels == null || !dirLevels.isDirectory()) {
			throw new URacerRuntimeException("Path not found (" + Storage.Levels + "), cannot check for available game levels.");
		}

		// check for any level
		FileHandle[] tracks = dirLevels.list("tmx");
		if (tracks.length == 0) {
			throw new URacerRuntimeException("Cannot find game levels.");
		}

		// build internal maps
		for (int i = 0; i < tracks.length; i++) {
			GameLevelDescriptor desc = computeDescriptor(tracks[i].name());
			levels.add(desc);

			// build lookup table
			levelIdToDescriptor.put(desc.getId(), desc);

			Gdx.app.log("GameLevels", "Found level \"" + desc.getName() + "\" (" + desc.getId() + ")");
		}

		// sort tracks
		Collections.sort(levels);

		return true;
	}

	public static TiledMap load (String levelId) {
		GameLevelDescriptor desc = getLevel(levelId);
		if (desc != null) {
			String filename = desc.getFileName();
			if (filename != null) {
				FileHandle h = Gdx.files.internal(Storage.Levels + filename);
				if (h.exists()) {
					return mapLoader.load(h.path(), mapLoaderParams);
				}
			}
		}

		return null;
	}

	public static GameLevelDescriptor[] getLevels () {
		if (levelsCache == null) {
			levelsCache = levels.toArray(new GameLevelDescriptor[levels.size()]);
		}

		return levelsCache;
	}

	public static GameLevelDescriptor getLevel (String levelId) {
		return levelIdToDescriptor.get(levelId);
	}

	public static boolean levelIdExists (String levelId) {
		return (levelIdToDescriptor.get(levelId) != null);
	}

	/** Compute a checksum on the level data, specifically on the tile positions and their order */
	private static GameLevelDescriptor computeDescriptor (String filename) {
		String filePath = Storage.Levels + filename;
		Element root = null;

		try {
			root = xml.parse(Gdx.files.internal(filePath));
		} catch (IOException e) {
			throw new URacerRuntimeException("Error reading level \"" + filePath + "\"");
		}

		if (root == null) {
			throw new URacerRuntimeException("Level \"" + filePath + "\" looks like it's corrupted.");
		}

		// retrieve layer and its name
		Element layer = root.getChildByName("layer");
		String layerName = layer.getAttribute("name", "");

		// check for a "track" layer
		if (!layerName.equals("track")) {
			throw new URacerRuntimeException("Level \"" + filePath
				+ "\" is not a valid uRacer level definition, no track layer found.");
		}

		// retrieve layer size
		int width = layer.getIntAttribute("width", 0);
		int height = layer.getIntAttribute("height", 0);

		if (width <= 0 || height <= 0) {
			throw new URacerRuntimeException("Level \"" + filePath
				+ "\" is not a valid uRacer level definition, no size on either width or height.");
		}

		Element data = layer.getChildByName("data");
		Element props = root.getChildByName("properties");

		// retrieve name, if any
		String levelName = "";
		for (Element property : props.getChildrenByName("property")) {
			String name = property.getAttribute("name", null);
			String value = property.getAttribute("value", null);
			if (name.equals("name")) {
				levelName = value;
				break;
			}
		}

		// check for unnamed track
		if (levelName.length() == 0) {
			throw new URacerRuntimeException("Level \"" + filePath + "\" is not a valid uRacer level definition, unnamed track.");
		}

		// check for the right encoding and compression
		String encoding = data.getAttribute("encoding", null);
		String compression = data.getAttribute("compression", null);
		if (!encoding.equals("base64")) {
			throw new URacerRuntimeException("Level \"" + filePath
				+ "\" is not a valid uRacer level definition, not base64-encoded.");
		}

		if (!compression.equals("zlib")) {
			throw new URacerRuntimeException("Level \"" + filePath + "\" is not a valid uRacer level definition, not zlib-packed.");
		}

		byte[] temp = new byte[4];
		ByteBuffer out = ByteBuffer.allocate(width * height * 4);
		int outIdx = 0;

		// base64 decode source data
		byte[] trackData = Base64Coder.decode(data.getText());

		// unpack and costruct the data to be checksummed
		Inflater zlib = new Inflater();
		zlib.setInput(trackData, 0, trackData.length);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				try {
					zlib.inflate(temp, 0, 4);

					//@off
					int id = 
						unsignedByteToInt(temp[0]) | 
						unsignedByteToInt(temp[1]) << 8 | 
						unsignedByteToInt(temp[2]) << 16 |
						unsignedByteToInt(temp[3]) << 24;
					//@on

					// clear it
					id = id & ~0xE0000000;

					out.putInt((outIdx++) << 2, id);
				} catch (DataFormatException e) {
					throw new URacerRuntimeException("Level \"" + filePath
						+ "\" is not a valid uRacer level definition, unexpected data format.");
				}
			}
		}

		// compute checksum
		byte[] checksum;
		byte[] bLevelName;

		bLevelName = levelName.getBytes();
		DigestUtils.sha256.reset();
		DigestUtils.sha256.update(bLevelName);
		DigestUtils.sha256.update(out);
		checksum = DigestUtils.sha256.digest();

		temp = null;
		out = null;
		trackData = null;

		return new GameLevelDescriptor(levelName, new BigInteger(1, checksum), filename);
	}

	private static int unsignedByteToInt (byte b) {
		return (int)b & 0xFF;
	}

	private GameLevels () {
	}
}
