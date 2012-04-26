package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.utils.Manager;

/** Provides a way to capture the rendered scene to an off-screen buffer
 * and to apply a chain of effects on it before rendering to screen.
 *
 * Effects can be added or removed via {@link #addEffect(PostProcessorEffect)} and
 * {@link #removeEffect(PostProcessorEffect)}.
 *
 * @author bmanuel */
public final class PostProcessor implements Disposable {
	private final PingPongBuffer composite;
	private static Format fbFormat;
	private final Manager<PostProcessorEffect> manager = new Manager<PostProcessorEffect>();
	private final Array<PingPongBuffer> buffers = new Array<PingPongBuffer>( 5 );
	private final Color clearColor = Color.CLEAR;

	private boolean enabled = true;
	private boolean capturing = false;
	private boolean hasCaptured = false;

	// maintains a per-frame updated list of enabled effects
	private Array<PostProcessorEffect> enabledEffects = null;

	/** Construct a new PostProcessor with the given parameters. */
	public PostProcessor( int fboWidth, int fboHeight, boolean useDepth, boolean useAlphaChannel, boolean use32Bits ) {
		if( use32Bits ) {
			if( useAlphaChannel ) {
				fbFormat = Format.RGBA8888;
			} else {
				fbFormat = Format.RGB888;
			}
		} else {
			if( useAlphaChannel ) {
				fbFormat = Format.RGBA4444;
			} else {
				fbFormat = Format.RGB565;
			}
		}

		composite = newPingPongBuffer( fboWidth, fboHeight, fbFormat, useDepth );
		capturing = false;
		hasCaptured = false;
	}

	/** Construct a new PostProcessor with FBO dimensions set to the size of the
	 * screen */
	public PostProcessor( boolean useDepth, boolean useAlphaChannel, boolean use32Bits ) {
		this( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), useDepth, useAlphaChannel, use32Bits );
	}

	/** Creates and returns a managed PingPongBuffer buffer, just create and
	 * forget.
	 * This is a drop-in replacement for the same-signature PingPongBuffer's
	 * constructor. */
	public PingPongBuffer newPingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth ) {
		PingPongBuffer buffer = new PingPongBuffer( width, height, frameBufferFormat, hasDepth );
		buffers.add( buffer );
		return buffer;
	}

	/** Frees owned resources. */
	@Override
	public void dispose() {
		manager.dispose();

		// cleanup managed buffers, if any
		for( int i = 0; i < buffers.size; i++ ) {
			buffers.get( i ).dispose();
		}

		buffers.clear();

		if( enabledEffects != null ) {
			enabledEffects.clear();
		}
	}

	/** Whether or not the post-procesor is enabled */
	public boolean isEnabled() {
		return enabled;
	}

	/** Sets whether or not the post-processor should be enabled */
	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}

	/** Adds the specified effect to the effect chain: the order IS important
	 * since effects will be applied in a FIFO fashion, the first added
	 * is the first being applied. */
	public void addEffect( PostProcessorEffect effect ) {
		manager.add( effect );
	}

	/** Removes the specified effect from the effect chain. */
	public void removeEffect( PostProcessorEffect effect ) {
		manager.remove( effect );
	}

	/** Returns the internal framebuffer format, computed from the
	 * parameters specified during construction.
	 * NOTE: this static will be valid from upon construction and NOT early! */
	public static Format getFramebufferFormat() {
		return fbFormat;
	}

	/** Sets the color that will be used to clear the buffer. */
	public void setClearColor( Color color ) {
		clearColor.set( color );
	}

	/** Sets the color that will be used to clear the buffer. */
	public void setClearColor( float r, float g, float b, float a ) {
		clearColor.set( r, g, b, a );
	}

	/** Starts capturing the scene, clears the buffer with the clear
	 * color specified by {@link #setClearColor(Color)} or {@link #setClearColor(float r, float g, float b, float a)}.
	 *
	 * @return true or false, whether or not capturing has been initiated. */
	public boolean capture() {
		if( enabled && !capturing ) {
			if( buildEnabledEffectsList() == 0 ) {
				// no enabled effects
				// Gdx.app.log( "PostProcessor::capture()", "No post-processor effects enabled" );
				return false;
			}

			capturing = true;
			hasCaptured = false;
			composite.begin();
			composite.capture();

			Gdx.gl.glClearColor( clearColor.r, clearColor.g, clearColor.b, clearColor.a );
			Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
			return true;
		}

		return false;
	}

	/** Starts capturing the scene as {@link #capture()}, but <strong>without</strong> clearing the screen.
	 *
	 * @return true or false, whether or not capturing has been initiated. */
	public boolean captureNoClear() {
		if( enabled && !capturing ) {
			if( buildEnabledEffectsList() == 0 ) {
				// no enabled effects
				// Gdx.app.log( "PostProcessor::captureNoClear", "No post-processor effects enabled" );
				return false;
			}

			capturing = true;
			hasCaptured = false;
			composite.begin();
			composite.capture();
			return true;
		}

		return false;
	}

	/** Stops capturing the scene and returns the result, or null if nothing was
	 * captured. */
	public FrameBuffer captureEnd() {
		if( enabled && capturing ) {
			capturing = false;
			hasCaptured = true;
			composite.end();
			return composite.getResultBuffer();
		}

		return null;
	}

	/** After a capture/captureEnd action, returns the just captured buffer */
	public FrameBuffer captured() {
		if( enabled && hasCaptured ) {
			return composite.getResultBuffer();
		}

		return null;
	}

	/** Regenerates and/or rebinds owned resources when needed, eg. when
	 * the OpenGL context is lost. */
	public void rebind() {
		Array<PostProcessorEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).rebind();
		}

		for( int i = 0; i < buffers.size; i++ ) {
			buffers.get( i ).rebind();
		}
	}

	/** Stops capturing the scene and apply the effect chain, if there is one. */
	public void render() {
		if( captureEnd() == null ) {
			return;
		}

		// Array<PostProcessorEffect> items = manager.items;
		Array<PostProcessorEffect> items = enabledEffects;

		if( items.size > 0 ) {
			// render effects chain, [0,n-1]
			for( int i = 0; i < items.size - 1; i++ ) {
				PostProcessorEffect e = items.get( i );

				composite.capture();
				{
					e.render( composite.getSourceBuffer(), composite.getResultBuffer() );
				}
			}

			// complete
			composite.end();

			// render with null dest (to screen)
			items.get( items.size - 1 ).render( composite.getResultBuffer(), null );
		} else {
			Gdx.app.log( "PostProcessor", "No post-processor effects enabled, aborting render" );
		}
	}

	private int buildEnabledEffectsList() {
		if( enabledEffects == null ) {
			enabledEffects = new Array<PostProcessorEffect>( manager.items.size );
		} else {
			enabledEffects.clear();
		}

		Array<PostProcessorEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			PostProcessorEffect effect = items.get( i );
			if( effect.isEnabled() ) {
				enabledEffects.add( effect );
			}
		}

		return enabledEffects.size;
	}
}