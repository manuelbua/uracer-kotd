package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
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
import com.bitfire.uracer.postprocessing.effects.Tv;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;

public class AggressiveCold implements Animator {
	private GameWorld gameWorld;
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;
	private Tv tv = null;

	public AggressiveCold( GameWorld world, PostProcessing post ) {
		gameWorld = world;
		bloom = (Bloom)post.getEffect( "bloom" );
		zoom = (Zoom)post.getEffect( "zoom" );
		vignette = (Vignette)post.getEffect( "vignette" );
		tv = (Tv)post.getEffect( "tvlines" );

		reset();
	}

	@Override
	public final void reset() {
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
				vignette.setLutIndex( 16 );
				vignette.setEnabled( true );
			}

			if( Config.PostProcessing.EnableCrtScreen ) {
				startMs = TimeUtils.millis();
				tv.setTime( 0 );

				// FIXME should find out a way to compute this per-resolution! OR!
				// Just try manually the game and write down the offset, this should work well, no much resolutions anyway
				// and will work better since this is human-tested!
//				tv.setOffset( 0.00145f );	// 1920x1080
				tv.setOffset( 0.002f );	// 1920x1080
				tv.setDistortion( 0.125f );	// 1920x1080
//				tv.setTint( 0.95f, 0.8f, 1.0f );
				tv.setTint( 0.95f, 0.75f, 0.85f );
			}
		}
	}

	private float prevDriftStrength = 0;
	private long startMs = 0;

	@Override
	public void update( PlayerCar player, GhostCar ghost ) {
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

		if( Config.PostProcessing.EnableCrtScreen ) {
			// compute time (add noise)
			float secs = (float)(TimeUtils.millis() - startMs) / 1000;
			boolean randomNoiseInTime = false;
			if( randomNoiseInTime ) {
				tv.setTime( secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f) );
			} else {
				tv.setTime( secs );
			}

//			tv.setDistortion( 0.2f + timeFactor * 0.05f );
		}

		if( Config.PostProcessing.EnableZoomBlur && player != null ) {
			zoom.setOrigin( playerScreenPos );
			zoom.setStrength( -0.1f * player.carState.currSpeedFactor * timeFactor );
		}

		if( Config.PostProcessing.EnableBloom ) {
			bloom.setBaseSaturation( AMath.lerp( 0.8f, 0.3f, timeFactor ) );
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation( 1f - timeFactor * 0.25f );
			bloom.setBloomIntesity( 1f );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if( vignette.controlSaturation ) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation( 1 - timeFactor * 0.25f );
				vignette.setSaturationMul( 1f + timeFactor * 0.2f );
			}

			// vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
			// vignette.setCoords( 1.5f - driftStrength * 0.8f, 0.1f );

			vignette.setLutIntensity( 0.5f + timeFactor * 0.5f );
			vignette.setIntensity( timeFactor );
		}
	}
}
