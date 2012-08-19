
package com.bitfire.uracer.game.world.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.world.WorldDefs.ObjectGroup;
import com.bitfire.uracer.game.world.WorldDefs.TileLayer;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.VMath;

public final class MapUtils implements Disposable {

	// cache
	public static final Map<String, TiledLayer> cachedLayers = new HashMap<String, TiledLayer>(10);
	public static final Map<String, TiledObjectGroup> cachedGroups = new HashMap<String, TiledObjectGroup>(10);

	private TiledMap map;
	private Vector2 worldSizeScaledPx = new Vector2();
	public float scaledTilesize, invScaledTilesize;

	public MapUtils (TiledMap map, Vector2 worldSizeScaledPx, float invZoomFactor) {
		this.map = map;
		this.worldSizeScaledPx.set(worldSizeScaledPx);
		scaledTilesize = map.tileWidth * invZoomFactor;
		invScaledTilesize = 1f / scaledTilesize;
	}

	@Override
	public void dispose () {
		cachedGroups.clear();
		cachedLayers.clear();
	}

	public TiledObjectGroup getObjectGroup (ObjectGroup group) {
		TiledObjectGroup cached = cachedGroups.get(group.mnemonic);
		if (cached == null) {
			for (int i = 0; i < map.objectGroups.size(); i++) {
				TiledObjectGroup objgroup = map.objectGroups.get(i);
				if (objgroup.name.equals(group.mnemonic)) {
					cached = objgroup;
					break;
				}
			}

			cachedGroups.put(group.mnemonic, cached);
		}

		return cached;
	}

	public boolean hasObjectGroup (ObjectGroup group) {
		return getObjectGroup(group) != null;
	}

	public TiledLayer getLayer (TileLayer layer) {
		TiledLayer cached = cachedLayers.get(layer.mnemonic);
		if (cached == null) {
			for (int i = 0; i < map.layers.size(); i++) {
				TiledLayer tilelayer = map.layers.get(i);
				if (tilelayer.name.equals(layer.mnemonic)) {
					cached = tilelayer;
					break;
				}
			}

			cachedLayers.put(layer.mnemonic, cached);
		}

		return cached;
	}

	public boolean hasLayer (TileLayer layer) {
		return getLayer(layer) != null;
	}

	public static List<Vector2> extractPolyData (String encoded) {
		List<Vector2> ret = new ArrayList<Vector2>();

		if (encoded != null && encoded.length() > 0) {
			String[] pairs = encoded.split(" ");
			for (int j = 0; j < pairs.length; j++) {
				String[] pair = pairs[j].split(",");
				ret.add(new Vector2(Integer.parseInt(pair[0]), Integer.parseInt(pair[1])));
			}
		}

		return ret;
	}

	public Vector2 tileToMt (int tilex, int tiley) {
		return Convert.px2mt(tileToPx(tilex, tiley));
	}

	private Vector2 retTile = new Vector2();

	public Vector2 tileToPx (int tilex, int tiley) {
		retTile.set(tilex * 224 /** scaledTilesize */
		, (map.height - tiley) * 224 /** scaledTilesize */
		);
		return retTile;
	}

	public Vector2 pxToTile (float x, float y) {
		retTile.set(x, y);
		retTile.mul(invScaledTilesize);
		retTile.y = map.height - retTile.y;
		VMath.truncateToInt(retTile);
		return retTile;
	}

	private Vector2 retPx = new Vector2();

	public Vector2 mtToTile (float x, float y) {
		retPx.set(Convert.mt2px(x), Convert.mt2px(y));
		retPx = pxToTile(retPx.x, retPx.y);
		return retPx;
	}

	public Vector2 positionFor (Vector2 position) {
		return positionFor(position.x, position.y);
	}

	private Vector2 tmp = new Vector2();

	public Vector2 positionFor (float x, float y) {
		tmp = Convert.scaledPixels(tmp.set(x, y));
		tmp.y = worldSizeScaledPx.y - tmp.y;
		return tmp;
	}

	public float orientationFromDirection (String direction) {
		float ret = 0f;

		if (direction.equals("up")) {
			ret = 0f;
		} else if (direction.equals("right")) {
			ret = 90f;
		} else if (direction.equals("down")) {
			ret = 180f;
		} else if (direction.equals("left")) {
			ret = 270f;
		}

		return ret;
	}

}
