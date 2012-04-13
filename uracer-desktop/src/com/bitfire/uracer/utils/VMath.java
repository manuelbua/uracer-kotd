package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Vector math utils.
 *
 * @author manuel */

public final class VMath {
	private VMath() {
	}

	/** Returns a vector in a top-left coordinate system so that:
	 *
	 * up=[0,-1], left=[-1,0], right=[1,0], down=[0,1] */

	public static Vector2 fromRadians( Vector2 result, float radians ) {
		result.x = -MathUtils.sin( radians );
		result.y = -MathUtils.cos( radians );
		return result;
	}

	public static Vector2 fromDegrees( Vector2 result, float degrees ) {
		float radians = degrees * MathUtils.degreesToRadians;
		return VMath.fromRadians( result, radians );
	}

	public static float toAngle( Vector2 v ) {
		return MathUtils.atan2( v.x, -v.y );
	}

	public static Vector2 perp( Vector2 result, Vector2 perpAt ) {
		result.x = -perpAt.y;
		result.y = perpAt.x;
		return result;
	}

	// public static final Vector2 perp( Vector2 perpAt )
	// {
	// Vector2 result = new Vector2();
	// return VMath.perp( result, perpAt );
	// }

	public static Vector2 clamp( Vector2 v, float min, float max ) {
		v.x = AMath.clamp( v.x, min, max );
		v.y = AMath.clamp( v.y, min, max );
		return v;
	}

	public static Vector2 clamp( Vector2 v, float xmin, float xmax, float ymin, float ymax ) {
		v.x = AMath.clamp( v.x, xmin, xmax );
		v.y = AMath.clamp( v.y, ymin, ymax );
		return v;
	}

	public static Vector2 fixup( Vector2 v ) {
		if( (v.x * v.x + v.y * v.y) < AMath.CMP_EPSILON ) {
			v.x = 0;
			v.y = 0;
		}

		return v;
	}

	public static Vector2 truncate( Vector2 v, float maxLength ) {
		if( v.len() > maxLength ) {
			v.nor().mul( maxLength );
		}

		return v;
	}

	public static Vector2 truncateToInt( Vector2 v ) {
		v.x = (int)v.x;
		v.y = (int)v.y;
		return v;
	}
}
