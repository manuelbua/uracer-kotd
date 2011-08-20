package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;

public class MapUtils
{
	private static TiledMap map;

	public static void initialize(TiledMap map)
	{
		MapUtils.map = map;
	}

	public static TiledObjectGroup getObjectGroup(String groupName )
	{
		for( int i = 0; i < map.objectGroups.size(); i++ )
		{
			TiledObjectGroup group;
			group = map.objectGroups.get( i );
			if(group.name.equals( groupName ))
				return group;
		}

		return null;
	}

	public static boolean hasObjectGroup(String groupName)
	{
		return getObjectGroup( groupName ) != null;
	}
}
