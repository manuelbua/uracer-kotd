
package com.bitfire.uracer.entities;

import com.badlogic.gdx.utils.Disposable;

public abstract class Entity implements Disposable {
	// screen-coords
	protected EntityRenderState stateRender = new EntityRenderState();

	public EntityRenderState state () {
		return stateRender;
	}
}
