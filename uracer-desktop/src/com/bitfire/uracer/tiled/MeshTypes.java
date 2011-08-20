package com.bitfire.uracer.tiled;


public class MeshTypes
{
	public static final int MeshPalm = 1;
	public static final int MeshHouse = 2;
	public static final int MeshTribune = 3;
	public static final int MeshTower = 4;

	public static int fromString(String mesh)
	{
		if(mesh.equalsIgnoreCase( "palm" )) return MeshPalm;
		if(mesh.equalsIgnoreCase( "house" )) return MeshHouse;
		if(mesh.equalsIgnoreCase( "tribune" )) return MeshTribune;
		if(mesh.equalsIgnoreCase( "tower" )) return MeshTower;

		return 0;
	}
}
