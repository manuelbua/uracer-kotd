package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.post.animators.AggressiveCold;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.utils.Hash;

/** Encapsulates a post-processor animator that manages effects such as bloom and zoomblur to compose
 * and enhance the gaming experience. */
public class PostProcessing {

	private final GameWorld gameWorld;
	private final GameRenderer gameRenderer;
	private boolean canPostProcess = false;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	public LongMap<Animator> animators = new LongMap<Animator>();
	private Animator currentAnimator;

	// internally cached effects refs for faster access
	private Bloom bloom = null;
	private Zoom zoom = null;
	private Vignette vignette = null;
	// private CameraMotion cameraMotion = null;

	public PostProcessing( GameWorld gameWorld, GameRenderer gameRenderer ) {
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;

		canPostProcess = gameRenderer.hasPostProcessor();

		if( canPostProcess ) {
			configurePostProcessing( gameRenderer.getPostProcessor(), gameWorld );
			Gdx.app.log( "PostProcessing", "Post-processing enabled and configured" );
		}

		currentAnimator = null;
	}

	private void configurePostProcessing( PostProcessor processor, GameWorld world ) {

		processor.setEnabled( true );
		processor.setClearBits( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		processor.setClearDepth( 1f );

		if( Config.PostProcessing.EnableZoomBlur ) {
			zoom = new Zoom( Config.PostProcessing.ZoomQuality );
			zoom.setStrength( 0 );
			processor.addEffect( zoom );
			effects.put( Hash.APHash( "zoom" ), zoom );
		}

		// cameraMotion = new CameraMotion( gameRenderer.getDepthMap() );
		// processor.addEffect( cameraMotion );
		// effects.put( Hash.APHash( "cameramotion" ), cameraMotion );

		if( Config.PostProcessing.EnableBloom ) {
			bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

			// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1,
			// 1,
			// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
			// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1,
			// 1,
			// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

			processor.addEffect( bloom );
			effects.put( Hash.APHash( "bloom" ), bloom );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			vignette = new Vignette( Config.PostProcessing.EnableBloom ? false : true );
			processor.addEffect( vignette );
			effects.put( Hash.APHash( "vignette" ), vignette );
		}
	}

	public void createAnimators() {
		currentAnimator = new AggressiveCold( gameWorld, this );
		animators.put( Hash.APHash( "AggressiveCold" ), currentAnimator );

		// currentAnimator = new AggressiveWarm( gameWorld, this );
		// animators.put( Hash.APHash( "AggressiveWarm" ), currentAnimator );
	}

	public void addEffect( String name, PostProcessorEffect effect ) {
		effects.put( Hash.APHash( name ), effect );
	}

	public PostProcessorEffect getEffect( String name ) {
		return effects.get( Hash.APHash( name ) );
	}

	public GameRenderer getGameRenderer() {
		return gameRenderer;
	}

	public void enableAnimator( String name ) {
		Animator next = animators.get( Hash.APHash( name ) );
		if( next != null ) {
			if( currentAnimator != null ) {
				currentAnimator.reset();
			}

			currentAnimator = next;
		}
	}

	public void off() {
		if( currentAnimator != null ) {
			currentAnimator.reset();
			currentAnimator = null;
		}
	}

	public void onBeforeRender( PlayerCar player, GhostCar ghost ) {
		if( canPostProcess && currentAnimator != null ) {
			currentAnimator.update( player, ghost );
		}
	}
}
