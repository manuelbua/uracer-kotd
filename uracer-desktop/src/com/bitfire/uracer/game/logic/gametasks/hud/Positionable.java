
package com.bitfire.uracer.game.logic.gametasks.hud;

import com.badlogic.gdx.math.Vector2;

public abstract class Positionable {
	protected Vector2 position = new Vector2();
	protected Vector2 bounds = new Vector2();
	protected Vector2 halfBounds = new Vector2();

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

	public float getWidth () {
		return bounds.x;
	}

	public float getHalfWidth () {
		return halfBounds.x;
	}

	public float getHeight () {
		return bounds.y;
	}

	public float getHalfHeight () {
		return halfBounds.y;
	}
}
