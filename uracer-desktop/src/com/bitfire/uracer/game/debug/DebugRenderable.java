
package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.logic.gametasks.PlayerClient;

public abstract class DebugRenderable extends PlayerClient implements Disposable {
	public abstract void tick ();

	public void reset () {
	}

	public void render () {
	}

	public void renderBatch (SpriteBatch batch) {
	}
}
