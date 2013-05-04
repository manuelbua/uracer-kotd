
package com.bitfire.uracer.utils;

import java.util.HashSet;
import java.util.StringTokenizer;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/** Contains an atlas of tiles by tile id for use with {@link TileMapRenderer} */
public class TileAtlas {

	protected IntMap<TextureRegion> regionsMap = new IntMap<TextureRegion>();
	protected final HashSet<Texture> textures = new HashSet<Texture>(1);

	/** Protected constructor to allow different implementations */
	protected TileAtlas () {
	}

	/** Creates a TileAtlas for use with {@link TileMapRenderer}. Run the map through TiledMapPacker to create the files required.
	 * @param map The tiled map
	 * @param inputDir The directory containing all the files created by TiledMapPacker */
	public TileAtlas (TiledMap map, int firstgid, FileHandle inputDir, String imageName) {
		FileHandle packfile = getRelativeFileHandle(inputDir, removeExtension(imageName) + ".atlas");
		TextureAtlas textureAtlas = new TextureAtlas(packfile, packfile.parent(), false);
		Array<AtlasRegion> atlasRegions = textureAtlas.findRegions(removeExtension(removePath(imageName)));
		for (AtlasRegion reg : atlasRegions) {
			regionsMap.put(reg.index + firstgid, reg);
			if (!textures.contains(reg.getTexture())) {
				textures.add(reg.getTexture());
			}
		}
	}

	/** Gets an {@link TextureRegion} for a tile id
	 * @param id tile id
	 * @return the {@link TextureRegion} */
	public TextureRegion getRegion (int id) {
		return regionsMap.get(id);
	}

	public void setTextureFilter (TextureFilter min, TextureFilter mag) {
		for (Texture texture : textures) {
			texture.setFilter(min, mag);
		}
	}

	public Array<Texture> getTextures () {
		Array<Texture> ret = new Array<Texture>();
		for (Texture texture : textures) {
			ret.add(texture);
		}

		return ret;
	}

	private static String removeExtension (String s) {
		int extensionIndex = s.lastIndexOf(".");
		if (extensionIndex == -1) {
			return s;
		}

		return s.substring(0, extensionIndex);
	}

	private static String removePath (String s) {
		String temp;

		int index = s.lastIndexOf('\\');
		if (index != -1) {
			temp = s.substring(index + 1);
		} else {
			temp = s;
		}

		index = temp.lastIndexOf('/');
		if (index != -1) {
			return s.substring(index + 1);
		} else {
			return s;
		}
	}

	private static FileHandle getRelativeFileHandle (FileHandle path, String relativePath) {
		if (relativePath.trim().length() == 0) {
			return path;
		}

		FileHandle child = path;

		StringTokenizer tokenizer = new StringTokenizer(relativePath, "\\/");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals("..")) {
				child = child.parent();
			} else {
				child = child.child(token);
			}
		}

		return child;
	}
}
