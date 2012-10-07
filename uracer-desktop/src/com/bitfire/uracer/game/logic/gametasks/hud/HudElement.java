
package com.bitfire.uracer.game.logic.gametasks.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public abstract class HudElement implements Disposable {
	public void onTick () {
	}

	public void onReset () {
		// no impl
	}

	public abstract void onRender (SpriteBatch batch);
}
