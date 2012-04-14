package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.messager.Message.MessagePosition;
import com.bitfire.uracer.game.messager.Message.MessageSize;
import com.bitfire.uracer.game.messager.Message.Type;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.VMath;

public final class HudDrifting extends HudElement {
	// we need an HudLabel circular buffer since
	// the player could be doing combos and the time
	// needed for one single labelResult to ".slide"
	// and disappear could be higher than the time
	// needed for the user to initiate, perform and
	// finish the next drift.. in this case the label
	// will move from the last result position to the
	// current one
	private static final int MaxLabelResult = 3;

	private HudLabel labelRealtime;
	private HudLabel[] labelResult;

	private int nextLabelResult = 0;
	private float carModelWidthPx, carModelLengthPx;

	private Vector2 displacement = new Vector2();
	private Vector2 tmpv = new Vector2();
	private Vector2 lastRealtimePos = new Vector2();

	private DriftStateEvent.Listener driftListener = new DriftStateEvent.Listener() {
		@Override
		public void driftStateEvent( DriftStateEvent.Type type ) {
			switch( type ) {
			case onBeginDrift:
				onBeginDrift();
				break;
			case onEndDrift:
				onEndDrift();
				break;
			}
		}
	};

	private void onBeginDrift() {
		labelRealtime.fadeIn( 300 );
	}

	private void onEndDrift() {
		DriftState drift = GameData.States.drift;

		HudLabel result = labelResult[nextLabelResult++];
		if( nextLabelResult == MaxLabelResult ) {
			nextLabelResult = 0;
		}

		result.setPosition( lastRealtimePos );

		float driftSeconds = drift.driftSeconds();

		// premature end drift event due to collision?
		if( drift.hasCollided ) {
			result.setString( "-" + NumberString.format( driftSeconds ) );
			result.setFont( Art.fontCurseRbig );
		} else {
			result.setString( "+" + NumberString.format( driftSeconds ) );
			result.setFont( Art.fontCurseGbig );

			String seconds = NumberString.format( driftSeconds ) + "  seconds!";

			if( driftSeconds >= 1 && driftSeconds < 3f ) {
				GameData.Environment.messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
				GameData.Environment.messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 5f ) {
				GameData.Environment.messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, MessagePosition.Bottom, MessageSize.Big );
			}
		}

		result.slide();
		labelRealtime.fadeOut( 300 );
	}

	public HudDrifting( float carModelWidthPx, float carModelLengthPx ) {
		GameEvents.driftState.addListener( driftListener );

		this.carModelWidthPx = carModelWidthPx;
		this.carModelLengthPx = carModelLengthPx;

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99", 0.5f );
		labelRealtime.setAlpha( 0 );
		lastRealtimePos.set( 0, 0 );

		labelResult = new HudLabel[ MaxLabelResult ];
		nextLabelResult = 0;
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i] = new HudLabel( Art.fontCurseR, "99.99", 0.85f );
			labelResult[i].setAlpha( 0 );
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	void onReset() {
		labelRealtime.setAlpha( 0 );
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].setAlpha( 0 );
		}
		nextLabelResult = 0;
	}

	@Override
	void onRender( SpriteBatch batch, Vector2 playerPosition, float playerOrientation ) {
		DriftState drift = GameData.States.drift;

		tmpv.set( Director.screenPosForPx( playerPosition ) );

		// compute heading
		displacement.set( VMath.fromDegrees( playerOrientation ) );

		// compute displacement
		displacement.x *= (carModelWidthPx + labelRealtime.halfBoundsWidth);
		displacement.y *= (carModelLengthPx + labelRealtime.halfBoundsHeight);

		// displace the position
		tmpv.sub( displacement );

		labelRealtime.setPosition( tmpv );
		lastRealtimePos.set( tmpv );

		// draw earned/lost seconds
		if( labelRealtime.getAlpha() > 0 ) {
			labelRealtime.setString( "+" + NumberString.format( drift.driftSeconds() ) );
			labelRealtime.render( batch );
		}

		// draw result
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].render( batch );
		}
	}

}
