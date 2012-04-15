package com.bitfire.uracer.game.messager;

import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.messager.Message.MessagePosition;
import com.bitfire.uracer.game.messager.Message.MessageSize;
import com.bitfire.uracer.task.Task;

public class Messager extends Task {
	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			for( MessagePosition group : MessagePosition.values() ) {
				if( isBusy( group ) ) {
					currents.get( group.ordinal() ).render( batch );
				}
			}
		}
	};

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	// data
	private Array<LinkedList<Message>> messages;
	private Array<Message> currents;
	private Message[] messageStore;
	private static final int MaxMessagesInStore = 10;
	private int idxMessageStore;

	public Messager( float invZoomFactor ) {
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.MINUS_4 );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );

		currents = new Array<Message>( 3 );
		for( MessagePosition group : MessagePosition.values() ) {
			currents.insert( group.ordinal(), null );
		}

		messages = new Array<LinkedList<Message>>( 3 );
		for( MessagePosition group : MessagePosition.values() ) {
			messages.insert( group.ordinal(), new LinkedList<Message>() );
		}

		// initialize message store
		idxMessageStore = 0;
		messageStore = new Message[ MaxMessagesInStore ];
		for( int i = 0; i < MaxMessagesInStore; i++ ) {
			messageStore[i] = new Message( invZoomFactor );
		}
	}

	@Override
	public void dispose() {
		reset();
	}

	@Override
	protected void onTick() {
		update( MessagePosition.Top );
		update( MessagePosition.Middle );
		update( MessagePosition.Bottom );
	}

	public boolean isBusy( MessagePosition group ) {
		return (currents.get( group.ordinal() ) != null);
	}

	public void reset() {
		for( MessagePosition group : MessagePosition.values() ) {
			messages.get( group.ordinal() ).clear();
		}

		for( MessagePosition group : MessagePosition.values() ) {
			currents.set( group.ordinal(), null );
		}

		idxMessageStore = 0;
	}

	private void update( MessagePosition group ) {
		LinkedList<Message> msgs = messages.get( group.ordinal() );
		Message msg = currents.get( group.ordinal() );

		// any message?
		if( msg == null && (msgs.peek() != null) ) {
			// schedule next message to process
			msg = msgs.remove();
			currents.set( group.ordinal(), msg );
		}

		// busy or became busy?
		if( msg != null ) {
			// start message if needed
			if( !msg.started ) {
				msg.started = true;
				msg.startMs = System.currentTimeMillis();
				msg.onShow();
			}

			if( !msg.tick() ) {
				currents.set( group.ordinal(), null );
				return;
			}

			// check if finished
			if( (System.currentTimeMillis() - msg.startMs) >= msg.durationMs && !msg.isHiding() ) {
				// message should end
				msg.onHide();
			}
		}
	}

	public void show( String message, float durationSecs, Message.Type type, MessagePosition position, MessageSize size ) {
		if( isBusy( position ) ) {
			currents.get( position.ordinal() ).onHide();
		}

		enqueue( message, durationSecs, type, position, size );
	}

	public void enqueue( String message, float durationSecs, Message.Type type, MessagePosition position, MessageSize size ) {
		Message m = nextFreeMessage();
		m.set( message, durationSecs, type, position, size );
		messages.get( position.ordinal() ).add( m );
	}

	private Message nextFreeMessage() {
		Message ret = messageStore[idxMessageStore++];
		if( idxMessageStore == MaxMessagesInStore ) {
			idxMessageStore = 0;
		}

		return ret;
	}
}
