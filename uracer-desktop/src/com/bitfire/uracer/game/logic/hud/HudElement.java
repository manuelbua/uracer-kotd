package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public abstract class HudElement {
	protected Vector2 playerPosition = new Vector2();
	protected float playerOrientation;

	abstract void onTick();

	abstract void onReset();

	abstract void onRender( SpriteBatch batch );
}
