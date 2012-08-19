
package com.bitfire.uracer.game.collisions;

/** Defines entities' behavior upon collision: this follows the box2d rules (see b2WorldCallbacks.cpp)
 * 
 * bool b2ContactFilter::ShouldCollide(b2Fixture* fixtureA, b2Fixture* fixtureB) { const b2Filter& filterA =
 * fixtureA->GetFilterData(); const b2Filter& filterB = fixtureB->GetFilterData();
 * 
 * if (filterA.groupIndex == filterB.groupIndex && filterA.groupIndex != 0) { return filterA.groupIndex > 0; }
 * 
 * bool collide = (filterA.maskBits & filterB.categoryBits) != 0 && (filterA.categoryBits & filterB.maskBits) != 0; return
 * collide; }
 * 
 * @author manuel */
public final class CollisionFilters {
	public static final short GroupNoCollisions = -1;

	public static final short GroupPlayer = 0x0001;
	public static final short GroupReplay = -0x0002;
	public static final short GroupTrackWalls = 0x0003;

	public static final short CategoryPlayer = 0x0001;
	public static final short CategoryReplay = 0x0002;
	public static final short CategoryTrackWalls = 0x0004;

	public static final short MaskPlayer = CategoryTrackWalls;
	public static final short MaskReplay = CategoryTrackWalls;
	public static final short MaskWalls = CategoryPlayer | CategoryReplay;

	private CollisionFilters () {
	}
}
