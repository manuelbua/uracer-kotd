
package com.bitfire.uracer.game.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.collisions.CollisionFilters;

public final class Box2DFactory {

	private Box2DFactory () {
	}

	// public static void init() {
	// Box2DFactory.tmp1 = new Vector2();
	// Box2DFactory.tmp2 = new Vector2();
	// Box2DFactory.from = new Vector2();
	// Box2DFactory.to = new Vector2();
	// }

	/** Creates a circle object with the given position and radius. Resitution defaults to 0.6. */
	// public static Body createCircle( World world, float x, float y, float radius, boolean isStatic ) {
	// CircleShape sd = new CircleShape();
	// sd.setRadius( radius );
	//
	// BodyDef bd = new BodyDef();
	// bd.bullet = false;
	// bd.allowSleep = true;
	// bd.position.set( x, y );
	// Body body = world.createBody( bd );
	//
	// if( isStatic ) {
	// body.setType( BodyDef.BodyType.StaticBody );
	// } else {
	// body.setType( BodyDef.BodyType.DynamicBody );
	// }
	//
	// FixtureDef fdef = new FixtureDef();
	// fdef.shape = sd;
	// fdef.density = 1.0f;
	// fdef.friction = 0.3f;
	// fdef.restitution = 0.6f;
	// body.createFixture( fdef );
	//
	// // MassData md = new MassData();
	// // md.mass = 1000f;
	// // md.I = 0f;
	// // md.center.x = md.center.y = 0f;
	// // body.setMassData( md );
	//
	// // System.out.println( "createCircle, mass=" + body.getMass() );
	//
	// return body;
	// }

	/** Creates a wall by constructing a rectangle whose corners are (xmin,ymin) and (xmax,ymax), and rotating the box
	 * counterclockwise through the given angle. Restitution defaults to 0. */
	public static Body createWall (World world, float xmin, float ymin, float xmax, float ymax, float angle) {
		return createWall(world, xmin, ymin, xmax, ymax, angle, 0f);
	}

	/** Creates a wall by constructing a rectangle whose corners are (xmin,ymin) and (xmax,ymax), and rotating the box
	 * counterclockwise through the given angle, with specified restitution. */
	public static Body createWall (World world, float xmin, float ymin, float xmax, float ymax, float angle, float restitution) {
		float cx = (xmin + xmax) / 2;
		float cy = (ymin + ymax) / 2;
		float hx = (xmax - xmin) / 2;
		float hy = (ymax - ymin) / 2;
		if (hx < 0) {
			hx = -hx;
		}

		if (hy < 0) {
			hy = -hy;
		}

		PolygonShape wallshape = new PolygonShape();
		wallshape.setAsBox(hx, hy, new Vector2(0f, 0f), angle);

		FixtureDef fdef = new FixtureDef();
		fdef.shape = wallshape;
		fdef.density = 1.0f;
		fdef.friction = 0.02f;

		fdef.filter.groupIndex = CollisionFilters.GroupTrackWalls;
		fdef.filter.categoryBits = CollisionFilters.CategoryTrackWalls;
		fdef.filter.maskBits = CollisionFilters.MaskWalls;

		if (Config.Debug.TraverseWalls) {
			fdef.filter.groupIndex = CollisionFilters.GroupNoCollisions;
		}

		if (restitution > 0) {
			fdef.restitution = restitution;
		}

		BodyDef bd = new BodyDef();
		bd.position.set(cx, cy);
		Body wall = world.createBody(bd);
		wall.createFixture(fdef);
		wall.setType(BodyDef.BodyType.StaticBody);
		return wall;
	}

	/** Creates a segment-like thin wall with 0.05 thickness going from (x1,y1) to (x2,y2) */
	// public static Body createThinWall( World world, float x1, float y1, float x2, float y2, float restitution ) {
	// // determine center point and rotation angle for createWall
	// float cx = (x1 + x2) / 2;
	// float cy = (y1 + y2) / 2;
	// float angle = (float)Math.atan2( y2 - y1, x2 - x1 );
	// float mag = (float)Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) );
	// return createWall( world, cx - mag / 2, cy - 0.05f, cx + mag / 2, cy + 0.05f, angle, restitution );
	// }

	public static Body createWall (World world, Vector2 from, Vector2 to, float size, float restitution) {
		// determine center point and rotation angle for createWall
		float halfSize = size / 2f;
		float cx = (from.x + to.x) / 2;
		float cy = (from.y + to.y) / 2;
		float angle = (float)Math.atan2(to.y - from.y, to.x - from.x);
		float mag = (float)Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y));
		return createWall(world, cx - mag / 2, cy - halfSize, cx + mag / 2, cy + halfSize, angle, restitution);
	}

	// private static Vector2 tmp1;
	// private static Vector2 tmp2;
	// private static Vector2 from;
	// private static Vector2 to;

	/** @param unitCircleRadius
	 * @param offset
	 * @param tickness
	 * @param lumpLen
	 * @param angle
	 * @param steps
	 * @param rotationOffset describes the rotation offset for the two endpoints of the wall being constructed.
	 * @param restitution
	 * @param returnResult
	 * @return */
	// public static List<Body> createAngularWall( World world, Vector2 unitCircleRadius, Vector2 offset, float
	// tickness, float lumpLen, float angle, int steps,
	// Vector2 rotationOffset, float restitution, boolean returnResult ) {
	// List<Body> result = null;
	// if( returnResult ) {
	// result = new ArrayList<Body>();
	// }
	//
	// float halfTickness = tickness / 2f;
	// float angleStep = angle / (float)(steps);
	// float radStep = angleStep * MathUtils.degreesToRadians;
	// float cosStep = (float)Math.cos( radStep );
	// float sinStep = (float)Math.sin( radStep );
	// float tmpx, tmpy;
	//
	// tmp1.set( unitCircleRadius );
	// tmp1.x -= halfTickness * rotationOffset.x;
	//
	// tmp2.set( tmp1 );
	// tmp2.x += halfTickness * rotationOffset.y;
	// tmp2.y -= lumpLen;
	//
	// for( int step = 0; step < steps; step++ ) {
	// from.x = offset.x + tmp1.x;
	// from.y = offset.y - tmp1.y;
	// to.x = offset.x + tmp2.x;
	// to.y = offset.y - tmp2.y;
	//
	// Body body = Box2DFactory.createWall( world, from, to, tickness, 0 );
	// if( returnResult ) {
	// result.add( body );
	// }
	//
	// // rotate
	// tmpx = tmp1.x * cosStep - tmp1.y * sinStep;
	// tmpy = tmp1.x * sinStep + tmp1.y * cosStep;
	// tmp1.set( tmpx, tmpy );
	//
	// tmpx = tmp2.x * cosStep - tmp2.y * sinStep;
	// tmpy = tmp2.x * sinStep + tmp2.y * cosStep;
	// tmp2.set( tmpx, tmpy );
	// }
	//
	// return result;
	// }
}
