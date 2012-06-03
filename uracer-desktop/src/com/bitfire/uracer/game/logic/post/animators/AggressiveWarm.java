package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.post.Animator;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.postprocessing.filters.ZoomBlur.Quality;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;

public final class AggressiveWarm implements Animator {
	private GameRenderer gameRenderer = null;
	private GameWorld gameWorld;
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;
//	private CameraMotion cameramotion = null;

	public AggressiveWarm( GameWorld world, PostProcessing post ) {
		gameRenderer = post.getGameRenderer();
		gameWorld = world;
		bloom = (Bloom)post.getEffect( "bloom" );
		zoom = (Zoom)post.getEffect( "zoom" );
		vignette = (Vignette)post.getEffect( "vignette" );
//		cameramotion = (CameraMotion)post.getEffect( "cameramotion" );

		reset();
	}

	@Override
	public void reset() {
		if( Config.PostProcessing.Enabled ) {
			if( Config.PostProcessing.EnableBloom ) {
				float threshold = ((gameWorld.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
				Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
				bloom.setSettings( bloomSettings );
			}

			if( Config.PostProcessing.EnableVignetting ) {
				vignette.setCoords( 0.8f, 0.25f );
				vignette.setCenter( Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 );
				vignette.setLut( Art.postXpro );
				vignette.setEnabled( true );
			}
		}
	}

	private float prevDriftStrength = 0;

	@Override
	public void update( PlayerCar player, GhostCar ghost ) {
//		cameramotion.setMatrices( gameRenderer.getWorldRenderer().getInvProjView(), gameRenderer.getWorldRenderer().getPrevProjView() );

		if( player == null ) {
			return;
		}

		if( !Config.PostProcessing.Enabled ) {
			return;
		}

		float timeFactor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);
		Vector2 playerScreenPos = GameRenderer.ScreenUtils.worldPxToScreen( player.state().position );

		float driftStrength = AMath.clamp( AMath.lerp( prevDriftStrength, player.driftState.driftStrength, 0.01f ), 0, 1 );
		prevDriftStrength = driftStrength;


		if( Config.PostProcessing.EnableZoomBlur && player != null ) {
			zoom.setOrigin( playerScreenPos );
			if( Config.PostProcessing.ZoomQuality==Quality.VeryHigh) {
				zoom.setStrength( -0.06f * player.carState.currSpeedFactor );
			} else {
				zoom.setStrength( -0.03f  * player.carState.currSpeedFactor );
			}
		}

		if( Config.PostProcessing.EnableBloom ) {
			bloom.setBaseSaturation( 1f - timeFactor );
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation( 1f - timeFactor * 0.2f );
//			bloom.setThreshold( 0.5f );

			// testing camera motion
			bloom.setThreshold( AMath.lerp( 0.5f, 0.5f, timeFactor ) );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if( vignette.controlSaturation ) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation( 1f - timeFactor * 0.55f );
				vignette.setSaturationMul( 1f + timeFactor * 0.125f );
			}

//			 vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
//			 vignette.setCoords( 1.5f - driftStrength * 0.8f, 0.1f );

			vignette.setLutIntensity( 0.1f + timeFactor * 1.4f );
			vignette.setLutIndex( 7 );

			vignette.setIntensity( timeFactor );
		}
	}
}

