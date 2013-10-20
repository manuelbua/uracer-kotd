
package com.bitfire.uracer.entities;

import com.badlogic.gdx.utils.Disposable;

public abstract class Entity implements Disposable {
	// screen-coords
	protected EntityRenderState stateRender = new EntityRenderState();

	/** Returns the last interpolated entity state */
	public EntityRenderState state () {
		return stateRender;
	}
}
