package com.bitfire.uracer.game.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.entities.Car;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.DriftStateEvent.Type;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public class HudDrifting {
	private Car playerCar;
	private CarModel model;
	private int carWidthPx, carLengthPx;

	private HudLabel labelRealtime;
	private HudLabel[] labelResult;

	// we need an HudLabel circular buffer since
	// the player could be doing combos and the time
	// needed for one single labelResult to ".slide"
	// and disappear could be higher than the time
	// needed for the user to initiate, perform and
	// finish the next drift.. in this case the label
	// will move from the last result position to the
	// current one
	private static final int MaxLabelResult = 3;

	private int nextLabelResult = 0;

	private Vector2 heading = new Vector2();

	private DriftStateEvent.Listener driftListener = new DriftStateEvent.Listener() {
		@Override
		public void driftStateEvent( Type type ) {
			switch( type ) {
			case onBeginDrift:
				labelRealtime.fadeIn( 300 );
				break;
			case onEndDrift:
				DriftState drift = States.driftState;
				Vector2 pos = tmpv.set( Director.screenPosForPx( playerCar.state().position ) );

				labelRealtime.fadeOut( 300 );

				HudLabel result = labelResult[nextLabelResult++];
				if( nextLabelResult == MaxLabelResult )
					nextLabelResult = 0;

				result.setPosition( pos.x - heading.x * (carWidthPx + result.halfBoundsWidth), pos.y - heading.y * (carLengthPx + result.halfBoundsHeight) );

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
						GameData.messager.enqueue( "NICE ONE!\n+" + seconds, 1f, MessageType.Good, MessagePosition.Middle, MessageSize.Big );
					} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
						GameData.messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, MessageType.Good, MessagePosition.Middle, MessageSize.Big );
					} else if( driftSeconds >= 5f ) {
						GameData.messager.enqueue( "UNREAL!\n+" + seconds, 1f, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
					}
				}

				result.slide();
				break;
			}
		}
	};

	public HudDrifting( Car car ) {
		Events.driftState.addListener( driftListener );

		this.playerCar = car;
		model = playerCar.getCarModel();
		carWidthPx = (int)Convert.mt2px( model.width );
		carLengthPx = (int)Convert.mt2px( model.length );

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99", 0.5f );
		labelRealtime.setAlpha( 0 );

		labelResult = new HudLabel[ MaxLabelResult ];
		nextLabelResult = 0;
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i] = new HudLabel( Art.fontCurseR, "99.99", 0.85f );
			labelResult[i].setAlpha( 0 );
		}
	}

	public void reset() {
		labelRealtime.setAlpha( 0 );
		for( int i = 0; i < MaxLabelResult; i++ )
			labelResult[i].setAlpha( 0 );
		nextLabelResult = 0;
	}

	private Vector2 tmpv = new Vector2();
	private float lastDistance = 0f;

	public void tick() {
		heading.set( playerCar.getSimulator().heading );
	}

	public void render( SpriteBatch batch ) {
		DriftState drift = States.driftState;

		// update from subframe-interpolated player position
		Vector2 pos = tmpv.set( Director.screenPosForPx( playerCar.state().position ) );

		// float secRatio = 1f;
		// float distance = 0f;
		if( drift.isDrifting ) {
			// secRatio = AMath.clamp( (System.currentTimeMillis() - drift.driftStartTime) / 2000f, 0, 1);
			// labelRealtime.setAlpha( secRatio );
			// distance = (1f-secRatio) * 50f;
			// lastDistance = distance;
			lastDistance = 0;
		}

		labelRealtime.setPosition(
		// offset by heading.mul(distance factor)
				pos.x - heading.x * (carWidthPx + labelRealtime.halfBoundsWidth + lastDistance), pos.y - heading.y
						* (carLengthPx + labelRealtime.halfBoundsHeight + lastDistance) );

		//
		// draw earned/lost seconds
		//
		labelRealtime.setString( "+" + NumberString.format( drift.driftSeconds() ) );
		labelRealtime.render( batch );

		//
		// draw result
		//
		for( int i = 0; i < MaxLabelResult; i++ )
			labelResult[i].render( batch );
	}

}
