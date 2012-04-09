package com.bitfire.uracer.game.messager;

import java.util.LinkedList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.task.Task;

public class Messager extends Task {
	public enum MessageType {
		Information, Bad, Good
	}

	public enum MessagePosition {
		Top, Middle, Bottom
	}

	public enum MessageSize {
		Normal, Big
	}

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( Type type ) {
			SpriteBatch batch = Events.gameRenderer.batch;

			if( isBusy( MessagePosition.Top ) ) {
				currents.get( MessagePosition.Top.ordinal() ).render( batch );
			}
			if( isBusy( MessagePosition.Middle ) ) {
				currents.get( MessagePosition.Middle.ordinal() ).render( batch );
			}
			if( isBusy( MessagePosition.Bottom ) ) {
				currents.get( MessagePosition.Bottom.ordinal() ).render( batch );
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
		Events.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.MINUS_4 );
		Events.gameLogic.addListener( gameLogicEvent );

		currents = new Array<Message>( 3 );
		currents.insert( MessagePosition.Top.ordinal(), null );
		currents.insert( MessagePosition.Middle.ordinal(), null );
		currents.insert( MessagePosition.Bottom.ordinal(), null );

		messages = new Array<LinkedList<Message>>( 3 );
		messages.insert( MessagePosition.Top.ordinal(), new LinkedList<Message>() );
		messages.insert( MessagePosition.Middle.ordinal(), new LinkedList<Message>() );
		messages.insert( MessagePosition.Bottom.ordinal(), new LinkedList<Message>() );

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
		messages.get( MessagePosition.Top.ordinal() ).clear();
		messages.get( MessagePosition.Middle.ordinal() ).clear();
		messages.get( MessagePosition.Bottom.ordinal() ).clear();

		currents.set( MessagePosition.Top.ordinal(), null );
		currents.set( MessagePosition.Middle.ordinal(), null );
		currents.set( MessagePosition.Bottom.ordinal(), null );

		idxMessageStore = 0;
	}

	private void update( MessagePosition group ) {
		LinkedList<Message> msgs = messages.get( group.ordinal() );
		Message msg = currents.get( group.ordinal() );

		// any message?
		if( msg == null && (msgs.peek() != null) ) {
			// schedule this message to be processed next
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

	public void show( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size ) {
		if( isBusy( position ) ) {
			currents.get( position.ordinal() ).onHide();
		}

		enqueue( message, durationSecs, type, position, size );
	}

	public void enqueue( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size ) {
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
