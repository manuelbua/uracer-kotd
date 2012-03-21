package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class EntityState {
	public float orientation = 0;
	public Vector2 position = new Vector2();
	private static EntityState result = new EntityState();

	public EntityState() {
		orientation = 0;
		position.x = position.y = 0;
	}

	public EntityState( EntityState state ) {
		set( state );
	}

	public EntityState( Vector2 position, float orientation ) {
		set( position, orientation );
	}

	public void set( EntityState state ) {
		this.orientation = state.orientation;
		this.position.set( state.position );
	}

	public void set( Vector2 position, float orientation ) {
		this.position.set( position );
		this.orientation = orientation;
	}

	public static EntityState interpolate( EntityState previous, EntityState current, float alpha ) {
		result.position.set( previous.position );
		result.position.set( result.position.lerp( current.position, alpha ) );
		// result.orientation = current.orientation * alpha + previous.orientation * (1 - alpha);

		float curr = current.orientation;
		float prev = previous.orientation;

		boolean needWrap = ((curr > 0 && prev < 0) || (prev > 0 && curr < 0)) && (Math.abs( curr ) + Math.abs( prev ) > 1f);
		if( needWrap ) {
			if( prev < 0 ) {
				prev += AMath.TWO_PI;
			}
			else {
				curr += AMath.TWO_PI;
			}

			result.orientation = curr * alpha + prev * (1 - alpha);
			result.orientation = -(AMath.TWO_PI - result.orientation);

			// Debug.print( "curr=%.4f, prev=%.4f, res=%.4f", current.orientation, previous.orientation, result.orientation );
		}
		else {
			result.orientation = current.orientation * alpha + previous.orientation * (1 - alpha);
		}

		return result;
	}

	public void toPixels() {
		this.position.x = Convert.mt2px( this.position.x );
		this.position.y = Convert.mt2px( this.position.y );
		this.orientation = this.orientation * MathUtils.radiansToDegrees;
	}

	public void toMeters() {
		this.position.x = Convert.px2mt( this.position.x );
		this.position.y = Convert.px2mt( this.position.y );
		this.orientation = this.orientation * MathUtils.degreesToRadians;
	}

	@Override
	public String toString() {
		return position.toString() + ", orient=" + orientation;
	}
}
