
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.tiled.TileAtlas;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;

public class UTileAtlas extends TileAtlas {

	public UTileAtlas (TiledMap map, FileHandle inputDir) {
		super(map, inputDir);
	}

	public void setFilter (TextureFilter minFilter, TextureFilter magFilter) {
		for (Texture t : this.textures) {
			t.setFilter(minFilter, magFilter);
		}
	}

	public void setWrap (TextureWrap u, TextureWrap v) {
		for (Texture t : this.textures) {
			t.setWrap(u, v);
		}
	}
}
