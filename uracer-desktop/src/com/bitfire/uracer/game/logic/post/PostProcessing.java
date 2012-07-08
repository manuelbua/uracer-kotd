package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.PostProcessorEffect;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Curvature;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.utils.Hash;

/** Encapsulates a post-processor animator that manages effects such as bloom and
 * zoomblur to compose
 * and enhance the gaming experience. */
public final class PostProcessing {

	public enum Effects {
		Zoomer, Bloom, Vignette, Crt, Curvature;

		public String name;

		private Effects() {
			name = this.toString();
		}
	}

	private boolean hasPostProcessor;
	private final PostProcessor postProcessor;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	public LongMap<PostProcessingAnimator> animators = new LongMap<PostProcessingAnimator>();
	private PostProcessingAnimator currentAnimator;

	public PostProcessing( PostProcessor postProcessor ) {
		this.postProcessor = postProcessor;
		hasPostProcessor = (this.postProcessor != null);

		if( hasPostProcessor ) {
			configurePostProcessor( postProcessor );
			currentAnimator = null;
		}
	}

	/** Creates the effects that will be available to the animators/manipulators
	 * to use, remember that the ownership of the instantiated objects is
	 * transfered to the PostProcessor when adding the effect to it. */
	private void configurePostProcessor( PostProcessor postProcessor ) {
		postProcessor.setEnabled( true );
		postProcessor.setClearBits( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		postProcessor.setClearDepth( 1f );
		postProcessor.setBufferTextureWrap( TextureWrap.ClampToEdge, TextureWrap.ClampToEdge );

		if( UserPreferences.bool( Preference.Zoom ) ) {
			RadialBlur.Quality rbq = RadialBlur.Quality.valueOf( UserPreferences.string( Preference.ZoomRadialBlurQuality ) );
			Zoomer z = (UserPreferences.bool( Preference.ZoomRadialBlur ) ? new Zoomer( rbq ) : new Zoomer());
			z.setBlurStrength( 0 );
			addEffect( Effects.Zoomer.name, z );
		}

		if( UserPreferences.bool( Preference.Bloom ) ) {
			addEffect( Effects.Bloom.name, new Bloom( Config.PostProcessing.ScaledFboWidth, Config.PostProcessing.ScaledFboHeight ) );
		}

		if( UserPreferences.bool( Preference.Vignetting ) ) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			addEffect( Effects.Vignette.name, new Vignette( !UserPreferences.bool( Preference.Bloom ) ) );
		}

		if( UserPreferences.bool( Preference.CrtScreen ) ) {
			addEffect( Effects.Crt.name, new CrtMonitor( UserPreferences.bool( Preference.Curvature ), false ) );
		} else if( UserPreferences.bool( Preference.Curvature ) ) {
			addEffect( Effects.Curvature.name, new Curvature() );
		}

		Gdx.app.log( "PostProcessing", "Post-processing enabled and configured" );
	}

	public void addEffect( String name, PostProcessorEffect effect ) {
		if( hasPostProcessor ) {
			postProcessor.addEffect( effect );
			effects.put( Hash.APHash( name ), effect );
		}
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
		if( !hasPostProcessor ) {
			return;
		}

		PostProcessingAnimator next = animators.get( Hash.APHash( name ) );
		if( next != null ) {
			currentAnimator = next;
			currentAnimator.reset();
		}
	}

	public void disableAnimator() {
		if( hasPostProcessor && currentAnimator != null ) {
			currentAnimator.reset();
			currentAnimator = null;
		}
	}

	public void onBeforeRender() {
		if( hasPostProcessor && currentAnimator != null ) {
			currentAnimator.update();
		}
	}
}
