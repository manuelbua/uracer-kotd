package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class HudElement {
	abstract void onTick();
	abstract void onReset();
	abstract void onRender(SpriteBatch batch);
}
