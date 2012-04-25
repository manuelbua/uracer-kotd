package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public abstract class HudElement implements Disposable {
	public void onTick() {
	}

	public abstract void onReset();

	public abstract void onRender( SpriteBatch batch );
}
