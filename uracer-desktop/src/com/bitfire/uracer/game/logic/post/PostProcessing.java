package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;

/** Encapsulates a post-processor animator that manages effects such as bloom and zoomblur to compose
 * and enhance the gaming experience. */
public class PostProcessing {

	private final GameWorld gameWorld;
	private final GameRenderer gameRenderer;
	private boolean canPostProcess = false;

	// effects
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;

	public PostProcessing( GameWorld gameWorld, GameRenderer gameRenderer ) {
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;

		canPostProcess = gameRenderer.hasPostProcessor();

		if( canPostProcess ) {
			configurePostProcessing( gameRenderer.getPostProcessor(), gameWorld );
			Gdx.app.log( "PostProcessing", "Post-processing enabled and configured" );
		}

	}

	private void configurePostProcessing( PostProcessor processor, GameWorld world ) {

		processor.setEnabled( true );

		if( Config.PostProcessing.EnableZoomBlur ) {
			zoom = new Zoom( Config.PostProcessing.ZoomQuality );
			zoom.setStrength( 0 );
			processor.addEffect( zoom );
		}

		if( Config.PostProcessing.EnableBloom ) {
			bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

			// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1,
			// 1,
			// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
			// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1,
			// 1,
			// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

			float threshold = ((world.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
			bloom.setSettings( bloomSettings );

			processor.addEffect( bloom );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			vignette = new Vignette( Config.PostProcessing.EnableBloom ? false : true );
			vignette.setCoords( 0.75f, 0.4f );
			processor.addEffect( vignette );
		}
	}

	public void onBeforeRender( PlayerCar player ) {
		if( canPostProcess ) {
			float factor = 1 - (URacer.timeMultiplier - GameLogic.TimeMultiplierMin) / (Config.Physics.PhysicsTimeMultiplier - GameLogic.TimeMultiplierMin);

			if( Config.PostProcessing.EnableZoomBlur && player != null ) {
				zoom.setOrigin( GameRenderer.ScreenUtils.worldPxToScreen( player.state().position ) );
				zoom.setStrength( -0.1f * factor );
			}

			if( Config.PostProcessing.EnableBloom ) {
				bloom.setBaseSaturation( 0.5f - 0.5f * factor );
				// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
				bloom.setBloomSaturation( 1.5f - factor * 1.5f );	// TODO when completely discharged
				bloom.setBloomIntesity( 1f + factor * 1.75f );
			}

			if( Config.PostProcessing.EnableVignetting ) {
				// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );
				// vignette.setIntensity( 1f );

				if( vignette.controlSaturation ) {
					// go with the "poor man"'s time dilation fx
					vignette.setSaturation( 1 - factor );
					vignette.setSaturationMul( 1f + factor * 0.5f );
				}

				vignette.setIntensity( factor );
			}
		}
	}
}
