package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;

public class MapUtils
{
	// object group name and properties
	public static final String LayerStaticMeshes = "static-meshes";
	public static final String LayerTrack = "track";
	public static final String MeshScale = "scale";

	private static TiledMap map;

	public static void initialize( TiledMap map )
	{
		MapUtils.map = map;
	}

	public static TiledObjectGroup getObjectGroup( String groupName )
	{
		for( int i = 0; i < map.objectGroups.size(); i++ )
		{
			TiledObjectGroup group = map.objectGroups.get( i );
			if( group.name.equals( groupName ) ) return group;
		}

		return null;
	}

	public static boolean hasObjectGroup( String groupName )
	{
		return getObjectGroup( groupName ) != null;
	}

	public static TiledLayer getLayer( String layerName )
	{
		for( int i = 0; i < map.layers.size(); i++ )
		{
			TiledLayer layer = map.layers.get( i );
			if( layer.name.equals( layerName ) ) return layer;
		}

		return null;
	}

	public static boolean hasLayer(String layerName)
	{
		return getLayer( layerName ) != null;
	}
}
