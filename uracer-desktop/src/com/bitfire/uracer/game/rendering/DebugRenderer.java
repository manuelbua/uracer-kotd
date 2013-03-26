
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;

public abstract class DebugRenderer implements Disposable, GameRendererEvent.Listener {

	private boolean attached = false;

	public void attach () {
		if (!attached) {
			GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
			attached = true;
		}
	}

	public void detach () {
		if (attached) {
			GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
			attached = false;
		}
	}

	@Override
	public void dispose () {
		detach();
	}

	@Override
	public void gameRendererEvent (Type type, Order order) {
		render();
	}

	public abstract void render ();
}
