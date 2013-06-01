
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;

public abstract class DebugRenderer implements Disposable {

	private boolean attached = false;
	private final GameRendererEvent.Listener renderEvent = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			render();
		}
	};

	public void attach () {
		if (!attached) {
			GameEvents.gameRenderer.addListener(renderEvent, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
			attached = true;
		}
	}

	public void detach () {
		if (attached) {
			GameEvents.gameRenderer.removeListener(renderEvent, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
			attached = false;
		}
	}

	@Override
	public void dispose () {
		detach();
	}

	public abstract void render ();
}
