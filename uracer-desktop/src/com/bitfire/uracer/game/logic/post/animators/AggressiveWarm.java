package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.post.Animator;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.resources.Art;

public class AggressiveWarm implements Animator {
	private GameWorld gameWorld;
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;

	public AggressiveWarm( GameWorld world, PostProcessing post ) {
		bloom = (Bloom)post.getEffect( "bloom" );
		zoom = (Zoom)post.getEffect( "zoom" );
		vignette = (Vignette)post.getEffect( "vignette" );
	}

	@Override
	public void reset() {
		if( Config.PostProcessing.EnableBloom ) {
			float threshold = ((gameWorld.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
			bloom.setSettings( bloomSettings );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			vignette.setCoords( 0.75f, 0.4f );
			vignette.setCenter( Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 );
			vignette.setLut( Art.postXpro );
			vignette.setEnabled( true );
		}
	}

	@Override
	public void update( PlayerCar player ) {
		if( player == null ) {
			return;
		}

		float factor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);
		Vector2 playerScreenPos = GameRenderer.ScreenUtils.worldPxToScreen( player.state().position );

		if( Config.PostProcessing.EnableZoomBlur && player != null ) {
			zoom.setOrigin( playerScreenPos );
			zoom.setStrength( -0.05f * factor * player.driftState.driftStrength );
		}

		if( Config.PostProcessing.EnableBloom ) {
			bloom.setBaseSaturation( 0.5f - 0.15f * factor );
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation( 1.5f - factor * 1.5f );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if( vignette.controlSaturation ) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation( 1 - factor );
				vignette.setSaturationMul( 1f + factor * 0.5f );
			}

			vignette.setLutIntensity( factor );
			vignette.setLutIndex( 7 );
			vignette.setCoords( 0.8f, 0.45f );
			vignette.setIntensity( factor );

		}
	}

}
