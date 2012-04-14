package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.VMath;

public final class HudDrifting extends HudElement {
	public enum EndDriftType {
		GoodDrift, BadDrift
	}

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
	private boolean began = false;

	public HudDrifting( float carModelWidthPx, float carModelLengthPx ) {
		this.carModelWidthPx = carModelWidthPx;
		this.carModelLengthPx = carModelLengthPx;

		// 99.99, reserve some space and do not recompute bounds
		labelRealtime = new HudLabel( Art.fontCurseYRbig, "+10.99", 0.5f );
		labelRealtime.setAlpha( 0 );
		lastRealtimePos.set( 0, 0 );

		labelResult = new HudLabel[ MaxLabelResult ];
		nextLabelResult = 0;
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i] = new HudLabel( Art.fontCurseR, "+10.99", 0.85f );
			labelResult[i].setAlpha( 0 );
		}
	}

	@Override
	public void dispose() {
	}

	public void beginDrift() {
		labelRealtime.fadeIn( 300 );
		began = true;
	}

	public void update( DriftState drift ) {
		if( began && labelRealtime.isVisible() ) {
			labelRealtime.setString( "+" + NumberString.format( drift.driftSeconds() ) );
		}
	}

	public void endDrift( String message, EndDriftType type ) {
		began = false;

		HudLabel result = labelResult[nextLabelResult++];

		if( nextLabelResult == MaxLabelResult ) {
			nextLabelResult = 0;
		}

		result.setPosition( lastRealtimePos );

		switch( type ) {
		case BadDrift:
			result.setFont( Art.fontCurseRbig );
			break;
		case GoodDrift:
		default:
			result.setFont( Art.fontCurseGbig );
			break;
		}

		result.setString( message );
		result.slide();
		labelRealtime.fadeOut( 300 );
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
		if( labelRealtime.isVisible() ) {
			labelRealtime.render( batch );
		}

		// draw result
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].render( batch );
		}
	}
}
