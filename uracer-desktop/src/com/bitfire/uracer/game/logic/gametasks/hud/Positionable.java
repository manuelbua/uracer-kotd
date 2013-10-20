
package com.bitfire.uracer.game.logic.gametasks.hud;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/** Represents a positionable hud element */
public abstract class Positionable implements Disposable {
	protected Vector2 position = new Vector2();

	protected float scale;

	public Positionable () {
		setScale(1);
	}

	@Override
	public void dispose () {
	}

	public void setPosition (Vector2 position) {
		this.position.set(position);
	}

	public void setPosition (float x, float y) {
		this.position.set(x, y);
	}

	public Vector2 getPosition () {
		return position;
	}

	public float getX () {
		return position.x;
	}

	public float getY () {
		return position.y;
	}

	public void setX (float x) {
		position.x = x;
	}

	public void setY (float y) {
		position.y = y;
	}

	public float getScale () {
		return scale;
	}

	public void setScale (float scale) {
		this.scale = scale;
	}

	public abstract float getWidth ();

	public abstract float getHeight ();
}
