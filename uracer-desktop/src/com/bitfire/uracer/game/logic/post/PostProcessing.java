package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
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
import com.bitfire.uracer.postprocessing.effects.CrtMonitor;
import com.bitfire.uracer.postprocessing.effects.Curvature;
import com.bitfire.uracer.postprocessing.effects.Vignette;
import com.bitfire.uracer.postprocessing.effects.Zoomer;
import com.bitfire.uracer.utils.Hash;

/** Encapsulates a post-processor animator that manages effects such as bloom and zoomblur to compose
 * and enhance the gaming experience. */
public class PostProcessing {

	private final GameWorld gameWorld;
	private final PostProcessor postProcessor;
	private boolean canPostProcess = false;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	public LongMap<PostProcessingAnimator> animators = new LongMap<PostProcessingAnimator>();
	private PostProcessingAnimator currentAnimator;

	public PostProcessing( GameWorld gameWorld, GameRenderer gameRenderer ) {
		this.gameWorld = gameWorld;

		canPostProcess = gameRenderer.hasPostProcessor();

		if( canPostProcess ) {
			postProcessor = gameRenderer.getPostProcessor();
			configurePostProcessing();
			Gdx.app.log( "PostProcessing", "Post-processing enabled and configured" );
		} else {
			postProcessor = null;
		}

		currentAnimator = null;
	}

	private void configurePostProcessing() {

		postProcessor.setEnabled( true );
		postProcessor.setClearBits( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		postProcessor.setClearDepth( 1f );
		postProcessor.setBufferTextureWrap( TextureWrap.ClampToEdge, TextureWrap.ClampToEdge );

		if( Config.PostProcessing.EnableZoom ) {
			Zoomer z = ( Config.PostProcessing.EnableZoomRadialBlur ? new Zoomer( Config.PostProcessing.RadialBlurQuality ) : new Zoomer() );
			z.setBlurStrength( 0 );
			addEffect( "zoomer", z );
		}

		// experimental camera motion blur (need subframe-interpolated position, disable camera position's rounding
		// before using it!)
		// cameraMotion = new CameraMotion( gameRenderer.getDepthMap() );
		// processor.addEffect( cameraMotion );
		// effects.put( Hash.APHash( "cameramotion" ), cameraMotion );

		if( Config.PostProcessing.EnableBloom ) {
			addEffect( "bloom", new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight ));
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			addEffect( "vignette", new Vignette( Config.PostProcessing.EnableBloom ? false : true ) );
		}

		if( Config.PostProcessing.EnableCrtScreen ) {
			addEffect( "crt", new CrtMonitor( Config.PostProcessing.EnableRadialDistortion, false ));

		} else if( Config.PostProcessing.EnableRadialDistortion ) {
			addEffect( "curvature", new Curvature() );
		}
	}

	public void createAnimators() {
		currentAnimator = new AggressiveCold( gameWorld, this );
		animators.put( Hash.APHash( "AggressiveCold" ), currentAnimator );

		// currentAnimator = new AggressiveWarm( gameWorld, this );
		// animators.put( Hash.APHash( "AggressiveWarm" ), currentAnimator );
	}

	public void addEffect( String name, PostProcessorEffect effect ) {
		postProcessor.addEffect( effect );
		effects.put( Hash.APHash( name ), effect );
	}

	public PostProcessorEffect getEffect( String name ) {
		return effects.get( Hash.APHash( name ) );
	}

	public void enableAnimator( String name ) {
		PostProcessingAnimator next = animators.get( Hash.APHash( name ) );
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
