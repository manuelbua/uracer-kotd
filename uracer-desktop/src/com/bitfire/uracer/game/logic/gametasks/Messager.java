
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;

public class Messager extends GameTask {
	private static final GameRendererEvent.Type RenderEvent = GameRendererEvent.Type.BatchAfterPostProcessing;
	private static final GameRendererEvent.Order RenderOrder = GameRendererEvent.Order.MINUS_4;

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			for (Position group : Position.values()) {
				if (isBusy(group)) {
					for (Message m : messages.get(group.ordinal())) {
						if (!m.isCompleted()) {
							m.render(batch);
						}
					}
				}
			}
		}
	};

	// data
	private Array<Array<Message>> messages;
	private Message[] messageStore;
	private static final int MaxMessagesInStore = 10;
	private int idxMessageStore;

	public Messager () {
		GameEvents.gameRenderer.addListener(gameRendererEvent, RenderEvent, RenderOrder);

		messages = new Array<Array<Message>>(3);
		for (Position group : Position.values()) {
			messages.insert(group.ordinal(), new Array<Message>());
		}

		// initialize message store
		idxMessageStore = 0;
		messageStore = new Message[MaxMessagesInStore];
		for (int i = 0; i < MaxMessagesInStore; i++) {
			messageStore[i] = new Message();
		}
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(gameRendererEvent, RenderEvent, RenderOrder);
	}

	@Override
	protected void onTick () {
		update(Position.Top);
		update(Position.Middle);
		update(Position.Bottom);
	}

	public boolean isBusy (Position group) {
		Array<Message> msgs = messages.get(group.ordinal());
		return (msgs.size > 0 && msgs.first() != null);
	}

	@Override
	public void onGameRestart () {
		onGameReset();
	}

	@Override
	public void onGameReset () {
		for (Position group : Position.values()) {
			messages.get(group.ordinal()).clear();
		}

		idxMessageStore = 0;
	}

	private void update (Position group) {
		Array<Message> msgs = messages.get(group.ordinal());

		for (Message msg : msgs) {
			// any message?
			if (msg == null && (msgs.size > 0 && msgs.first() != null)) {
				// schedule next message to process
				msg = msgs.first();
				msgs.removeValue(msg, true);
			}

			// busy or became busy?
			if (msg != null && !msg.isCompleted()) {
				// start message if needed
				if (!msg.started) {
					msg.started = true;
					msg.startMs = System.currentTimeMillis();
					msg.show();
				}

				// check if finished
				long much = (long)((System.currentTimeMillis() - msg.startMs) /* URacer.timeMultiplier */);
				if (msg.isShowComplete() && much >= msg.durationMs) {
					// message should end
					msg.hide();
				}
			}
		}
	}

	public void show (String message, float durationSecs, Message.Type type, Position position, Size size) {
		if (isBusy(position)) {
			messages.get(position.ordinal()).first().hide();
		}

		enqueue(message, durationSecs, type, position, size);
	}

	public void enqueue (String message, float durationSecs, Message.Type type, Position position, Size size) {
		Message m = nextFreeMessage();
		m.set(message, durationSecs, type, position, size);
		messages.get(position.ordinal()).add(m);
	}

	private Message nextFreeMessage () {
		Message ret = messageStore[idxMessageStore++];
		if (idxMessageStore == MaxMessagesInStore) {
			idxMessageStore = 0;
		}

		return ret;
	}
}
