package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;
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

	public enum Effects {
		Zoomer, Bloom, Vignette, Crt, Curvature;

		public String name;

		private Effects() {
			name = this.toString();
		}
	}

	private final PostProcessor postProcessor;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	public LongMap<PostProcessingAnimator> animators = new LongMap<PostProcessingAnimator>();
	private PostProcessingAnimator currentAnimator;

	public PostProcessing( PostProcessor postProcessor ) {
		this.postProcessor = postProcessor;
		configurePostProcessor( postProcessor );
		currentAnimator = null;
	}

	public void configurePostProcessor( PostProcessor postProcessor ) {
		postProcessor.setEnabled( true );
		postProcessor.setClearBits( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		postProcessor.setClearDepth( 1f );
		postProcessor.setBufferTextureWrap( TextureWrap.ClampToEdge, TextureWrap.ClampToEdge );

		if( Config.PostProcessing.EnableZoom ) {
			Zoomer z = (Config.PostProcessing.EnableZoomRadialBlur ? new Zoomer( Config.PostProcessing.RadialBlurQuality ) : new Zoomer());
			z.setBlurStrength( 0 );
			addEffect( Effects.Zoomer.name, z );
		}

		if( Config.PostProcessing.EnableBloom ) {
			addEffect( Effects.Bloom.name, new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight ) );
		}

		if( Config.PostProcessing.EnableVignetting ) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			addEffect( Effects.Vignette.name, new Vignette( !Config.PostProcessing.EnableBloom ) );
		}

		if( Config.PostProcessing.EnableCrtScreen ) {
			addEffect( Effects.Crt.name, new CrtMonitor( Config.PostProcessing.EnableRadialDistortion, false ) );

		} else if( Config.PostProcessing.EnableRadialDistortion ) {
			addEffect( Effects.Curvature.name, new Curvature() );
		}

		Gdx.app.log( "PostProcessing", "Post-processing enabled and configured" );
	}

	public void addEffect( String name, PostProcessorEffect effect ) {
		postProcessor.addEffect( effect );
		effects.put( Hash.APHash( name ), effect );
	}

	public PostProcessorEffect getEffect( String name ) {
		return effects.get( Hash.APHash( name ) );
	}

	public void addAnimator( String name, PostProcessingAnimator animator ) {
		animators.put( Hash.APHash( name ), animator );
	}

	public PostProcessingAnimator getAnimator( String name ) {
		return animators.get( Hash.APHash( name ) );
	}

	public void enableAnimator( String name ) {
		PostProcessingAnimator next = animators.get( Hash.APHash( name ) );
		if( next != null ) {
			currentAnimator = next;
			currentAnimator.reset();
		}
	}

	public void disableAnimator() {
		if( currentAnimator != null ) {
			currentAnimator.reset();
			currentAnimator = null;
		}
	}

	public void onBeforeRender( PlayerCar player, GhostCar ghost ) {
		if( currentAnimator != null ) {
			currentAnimator.update( player, ghost );
		}
	}
}
