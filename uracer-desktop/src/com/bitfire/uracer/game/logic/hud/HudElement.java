package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

public abstract class HudElement implements Disposable {
	void onTick() {
	}

	abstract void onReset();

	abstract void onRender( SpriteBatch batch, Vector2 playerPosition, float playerOrientation );
}
