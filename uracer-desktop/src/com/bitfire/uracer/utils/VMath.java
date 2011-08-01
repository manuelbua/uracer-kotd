package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Vector math utils.
 *
 * @author manuel
 *
 */

public class VMath
{
	/**
	 *	NOTES
	 *
	 *	General formulas for up=(0,1)
	 *
	 *		Vector2 AngleToVector( float angle )
	 *		{
	 *			return new Vector2( Math.Cos(angle), Math.Sin(angle) );
	 *		}
	 *
	 *		float VectorToAngle( Vector2 vector )
	 *		{
	 *			return Math.Atan2( vector.Y, vector.X );
	 *		}
	 *
	 *	Need to reverse for up=(0,-1)
	 */

	public static final Vector2 fromAngle(Vector2 result, float degrees)
	{
		float rads = degrees * MathUtils.degreesToRadians;
		result.x = MathUtils.sin( rads );
		result.y = -MathUtils.cos( rads );
		return result;
	}

	public static final Vector2 fromAngle(float degrees)
	{
		Vector2 result = new Vector2();
		return VMath.fromAngle( result, degrees );
	}

	public static final float toAngle(Vector2 v)
	{
		return MathUtils.atan2( v.x, -v.y );
	}

	public static final Vector2 perp(Vector2 result, Vector2 perpAt)
	{
		result.x = -perpAt.y;
		result.y = perpAt.x;
		return result;
	}

	public static final Vector2 perp(Vector2 perpAt)
	{
		Vector2 result = new Vector2();
		return VMath.perp( result, perpAt );
	}

	public static final Vector2 fixup(Vector2 v)
	{
		if( (v.x*v.x+v.y*v.y)<AMath.CMP_EPSILON)
		{
			v.x = v.y = 0;
		}

		return v;
	}

	public static final Vector2 truncate(Vector2 v, float maxLength)
	{
		if(v.len() > maxLength)
		{
			v.nor().mul( maxLength );
		}

		return v;
	}

	public static final Vector2 truncateToInt(Vector2 v)
	{
		v.x = (int)v.x;
		v.y = (int)v.y;
		return v;
	}
}
