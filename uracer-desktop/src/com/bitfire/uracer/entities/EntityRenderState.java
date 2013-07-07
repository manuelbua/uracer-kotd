
package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public final class EntityRenderState {
	public float orientation = 0;
	public Vector2 position = new Vector2();
	private static EntityRenderState result = new EntityRenderState();

	public EntityRenderState () {
		orientation = 0;
		position.x = 0;
		position.y = 0;
	}

	public EntityRenderState (EntityRenderState state) {
		set(state);
	}

	public EntityRenderState (Vector2 position, float orientation) {
		set(position, orientation);
	}

	public void set (EntityRenderState state) {
		this.orientation = state.orientation;
		this.position.set(state.position);
	}

	public void set (Vector2 position, float orientation) {
		this.position.set(position);
		this.orientation = orientation;
	}

	/** Interpolate between the specified render states, by the specified quantity. */
	public static EntityRenderState interpolate (EntityRenderState previous, EntityRenderState current, float alpha) {
		result.position.set(previous.position);
		result.position.set(result.position.lerp(current.position, alpha));

		float curr = current.orientation;
		float prev = previous.orientation;

		boolean hasWrapped = ((curr > 0 && prev < 0) || (prev > 0 && curr < 0));
		boolean needWrap = hasWrapped && (Math.abs(curr) + Math.abs(prev) > 1f);

		if (needWrap) {
			if (prev < 0) {
				prev += AMath.TWO_PI;
			} else {
				curr += AMath.TWO_PI;
			}

			result.orientation = curr * alpha + prev * (1 - alpha);
			result.orientation = -(AMath.TWO_PI - result.orientation);

			// Debug.print( "curr=%.4f, prev=%.4f, res=%.4f", current.orientation, previous.orientation,
			// result.orientation );
		} else {
			result.orientation = current.orientation * alpha + previous.orientation * (1 - alpha);
		}

		return result;
	}

	/** Returns whether or not the specified render states are equal with a bias of AMath.CMP_EPSILON */
	public static boolean isEqual (EntityRenderState first, EntityRenderState second) {
		boolean xIsEqual = AMath.isZero((float)(Math.abs(first.position.x) - Math.abs(second.position.x)));

		if (!xIsEqual) {
			return false;
		}

		return AMath.isZero((float)(Math.abs(first.position.y) - Math.abs(second.position.y)));
	}

	/** Transform the world position from meters to pixels. */
	public void toPixels () {
		this.position.x = Convert.mt2px(this.position.x);
		this.position.y = Convert.mt2px(this.position.y);
		this.orientation = this.orientation * MathUtils.radiansToDegrees;
	}

	@Override
	public String toString () {
		return position.toString() + ", orient=" + orientation;
	}
}
