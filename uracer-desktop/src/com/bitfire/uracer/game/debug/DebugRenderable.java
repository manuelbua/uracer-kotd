
package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.logic.gametasks.PlayerClient;

public abstract class DebugRenderable extends PlayerClient implements Disposable {
	private RenderFlags flag;

	public DebugRenderable (RenderFlags flag) {
		this.flag = flag;
	}

	public RenderFlags getFlag () {
		return flag;
	}

	public abstract void tick ();

	public void reset () {
	}

	public void render () {
	}

	public void renderBatch (SpriteBatch batch) {
	}
}
