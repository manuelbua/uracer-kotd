package com.bitfire.uracer.entities;

public class CollisionFilters
{
	public static final short GroupNoCollisions = 	-1;

	public static final short GroupPlayer =			0x0001;
	public static final short GroupReplay = 		0x0002;
	public static final short GroupTrackWalls =		0x0003;

	public static final short CategoryPlayer = 		0x0001;
	public static final short CategoryReplay = 		0x0002;
	public static final short CategoryTrackWalls = 	0x0004;

	public static final short MaskPlayer = CategoryTrackWalls;
	public static final short MaskReplay = CategoryTrackWalls;
	public static final short MaskWalls = CategoryPlayer | CategoryReplay;
}
