package com.bitfire.uracer.game.logic.hud.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.hud.HudElement;
import com.bitfire.uracer.game.logic.hud.HudLabel;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.utils.Convert;
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

	private PlayerCar player;
	private Vector2 playerPosition = new Vector2();

	private Vector2 displacement = new Vector2();
	private Vector2 tmpv = new Vector2();
	private Vector2 lastRealtimePos = new Vector2();
	private boolean began = false;

	public HudDrifting( ScalingStrategy scalingStrategy, PlayerCar player ) {
		this.player = player;
		this.carModelWidthPx = Convert.mt2px( player.getCarModel().width );
		this.carModelLengthPx = Convert.mt2px( player.getCarModel().length );

		// 99.99, reserve some space and do not recompute bounds
		labelRealtime = new HudLabel( scalingStrategy, Art.fontCurseYRbig, "+10.99", 0.5f );
		labelRealtime.setAlpha( 0 );
		lastRealtimePos.set( 0, 0 );

		labelResult = new HudLabel[ MaxLabelResult ];
		nextLabelResult = 0;
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i] = new HudLabel( scalingStrategy, Art.fontCurseR, "+10.99", 0.85f );
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

	@Override
	public void onTick() {
		if( began && labelRealtime.isVisible() ) {
			labelRealtime.setString( "+" + NumberString.format( player.driftState.driftSeconds() ) );
		}
	}

	public void endDrift( String message, EndDriftType type ) {
		began = false;

		HudLabel result = labelResult[nextLabelResult++];

		if( nextLabelResult == MaxLabelResult ) {
			nextLabelResult = 0;
		}

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
		result.setPosition( lastRealtimePos );
		result.slide( type == EndDriftType.GoodDrift );

		labelRealtime.setString( "+" + NumberString.format( player.driftState.driftSeconds() ) );
		labelRealtime.fadeOut( 300 );
	}

	@Override
	public void onReset() {
		labelRealtime.setAlpha( 0 );
		for( int i = 0; i < MaxLabelResult; i++ ) {
			labelResult[i].setAlpha( 0 );
		}

		nextLabelResult = 0;
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		playerPosition.set( player.state().position );
		float playerOrientation = player.state().orientation;

		// compute heading
		displacement.set( VMath.fromDegrees( playerOrientation ) );

		// compute displacement
		displacement.x *= (carModelWidthPx + labelRealtime.halfBoundsWidth);
		displacement.y *= (carModelLengthPx + labelRealtime.halfBoundsHeight);

		// gets pixel position and then displaces it
		tmpv.set( Director.screenPosForPx( playerPosition ) );
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
