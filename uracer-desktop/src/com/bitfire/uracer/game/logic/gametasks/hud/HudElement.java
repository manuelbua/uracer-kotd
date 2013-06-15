
package com.bitfire.uracer.game.logic.gametasks.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.logic.gametasks.PlayerClient;

public abstract class HudElement extends PlayerClient implements Disposable {
	public void onTick () {
	}

	public void onRestart () {
	}

	public void onReset () {
	}

	public abstract void onRender (SpriteBatch batch, float cameraZoom);
}
