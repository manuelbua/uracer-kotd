package com.bitfire.uracer.utils;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.math.Vector2;

public class MapUtils {
	public static final String MeshScale = "scale";

	// known layer names
	public static final String LayerTrack = "track";
	public static final String LayerLights = "lights";
	public static final String LayerStaticMeshes = "static-meshes";
	public static final String LayerTrees = "trees";
	public static final String LayerWalls = "walls";

	// cache
	public static final HashMap<String, TiledLayer> cachedLayers = new HashMap<String, TiledLayer>( 10 );
	public static final HashMap<String, TiledObjectGroup> cachedGroups = new HashMap<String, TiledObjectGroup>( 10 );

	private static TiledMap map;

	public static void initialize( TiledMap map ) {
		MapUtils.map = map;
	}

	public static TiledObjectGroup getObjectGroup( String groupName ) {
		TiledObjectGroup cached = cachedGroups.get( groupName );
		if( cached == null ) {
			for( int i = 0; i < map.objectGroups.size(); i++ ) {
				TiledObjectGroup group = map.objectGroups.get( i );
				if( group.name.equals( groupName ) ) {
					cached = group;
					break;
				}
			}

			cachedGroups.put( groupName, cached );
		}

		return cached;
	}

	public static boolean hasObjectGroup( String groupName ) {
		return getObjectGroup( groupName ) != null;
	}

	public static TiledLayer getLayer( String layerName ) {
		TiledLayer cached = cachedLayers.get( layerName );
		if( cached == null ) {
			for( int i = 0; i < map.layers.size(); i++ ) {
				TiledLayer layer = map.layers.get( i );
				if( layer.name.equals( layerName ) ) {
					cached = layer;
					break;
				}
			}

			cachedLayers.put( layerName, cached );
		}

		return cached;
	}

	public static boolean hasLayer( String layerName ) {
		return getLayer( layerName ) != null;
	}

	public static ArrayList<Vector2> extractPolyData( String encoded ) {
		ArrayList<Vector2> ret = new ArrayList<Vector2>();

		if( encoded != null && encoded.length() > 0 ) {
			String[] pairs = encoded.split( " " );
			for( int j = 0; j < pairs.length; j++ ) {
				String[] pair = pairs[j].split( "," );
				ret.add( new Vector2( Integer.parseInt( pair[0] ), Integer.parseInt( pair[1] ) ) );
			}
		}

		return ret;
	}
}
