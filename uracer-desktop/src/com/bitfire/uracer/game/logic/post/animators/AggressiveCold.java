package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Curvature;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.TimeModulator;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessingAnimator;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;

public final class AggressiveCold implements PostProcessingAnimator {
	public static final String Name = "AggressiveCold";

	private boolean nightMode = false;
	private GameLogic logic = null;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;

	public AggressiveCold( GameLogic logic, PostProcessing post, boolean nightMode ) {
		this.nightMode = nightMode;
		this.logic = logic;
		bloom = (Bloom)post.getEffect( PostProcessing.Effects.Bloom.name );
		zoom = (Zoomer)post.getEffect( PostProcessing.Effects.Zoomer.name );
		vignette = (Vignette)post.getEffect( PostProcessing.Effects.Vignette.name );
		crt = (CrtMonitor)post.getEffect( PostProcessing.Effects.Crt.name );
		curvature = (Curvature)post.getEffect( PostProcessing.Effects.Curvature.name );

		reset();
	}

	@Override
	public void reset() {
		if( bloom != null ) {
			float threshold = (nightMode ? 0.2f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f,
					0.5f, 1f, 1.5f );
			bloom.setSettings( bloomSettings );
		}

		if( vignette != null ) {
			vignette.setCoords( 0.8f, 0.25f );
			vignette.setCenter( Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 );
			vignette.setLut( Art.postXpro );
			vignette.setLutIndex( 16 );
			vignette.setEnabled( true );
		}

		if( crt != null ) {
			startMs = TimeUtils.millis();
			crt.setTime( 0 );

			crt.setColorOffset( 0.002f );
			crt.setDistortion( 0.125f );
			crt.setZoom( 0.94f );

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint( 0.95f, 0.75f, 0.85f );
		}

		if( curvature != null ) {
			curvature.setDistortion( 0.125f );
			curvature.setZoom( 0.94f );
		}
	}

	private float prevDriftStrength = 0;
	private long startMs = 0;

	@Override
	public void update() {
		PlayerCar player = logic.getPlayer();

		if( player == null ) {
			return;
		}

		float timeFactor = 1 - (URacer.timeMultiplier - TimeModulator.MinTime) / (TimeModulator.MaxTime - TimeModulator.MinTime);
		Vector2 playerScreenPos = GameRenderer.ScreenUtils.worldPxToScreen( player.state().position );

		float driftStrength = AMath.clamp( AMath.lerp( prevDriftStrength, player.driftState.driftStrength, 0.01f ), 0, 1 );
		prevDriftStrength = driftStrength;

		if( crt != null ) {
			// compute time (add noise)
			float secs = (float)(TimeUtils.millis() - startMs) / 1000;
			boolean randomNoiseInTime = false;
			if( randomNoiseInTime ) {
				crt.setTime( secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f) );
			} else {
				crt.setTime( secs );
			}
		}

		if( zoom != null && player != null ) {
			zoom.setOrigin( playerScreenPos );

			zoom.setBlurStrength( -0.1f * driftStrength * timeFactor );
			zoom.setZoom( 1.0f + 0.5f * driftStrength * timeFactor );

			// zoom.setBlurStrength( -0.1f * driftStrength );
			// zoom.setZoom( 1.0f + 0.5f * driftStrength + 0.2f * timeFactor );
		}

		if( bloom != null ) {
			// bloom.setBaseSaturation( 0.8f - timeFactor * 0.6f );
			bloom.setBaseSaturation( AMath.lerp( 0.8f, 0.25f, timeFactor ) );
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation( 1f - timeFactor * 0.2f );
		}

		if( vignette != null ) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if( vignette.controlSaturation ) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation( 1 - timeFactor * 0.25f );
				vignette.setSaturationMul( 1 + timeFactor * 0.2f );
			}

			// vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
			// vignette.setCoords( 1.5f - driftStrength * 0.8f, 0.1f );

			vignette.setLutIntensity( timeFactor * 1.5f );
			vignette.setIntensity( timeFactor );
			vignette.setLutIndex( 6 );
		}
	}
}
