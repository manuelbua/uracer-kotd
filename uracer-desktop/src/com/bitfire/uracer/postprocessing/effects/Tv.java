package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.PingPongBuffer;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.Blur;
import com.bitfire.uracer.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.postprocessing.filters.Combine;
import com.bitfire.uracer.postprocessing.filters.CrtScreen;

public class Tv extends PostProcessorEffect {
	private PingPongBuffer pingPongBuffer = null;
	private FrameBuffer buffer = null;
	private CrtScreen crt;
	private Color tmptint = new Color();
	private Blur blur;
	private Combine combine;
	private boolean doblur;

	public Tv( boolean barrelDistortion, boolean performBlur ) {
		// the effect is designed to work on the whole screen area, no small/mid size tricks!
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		doblur = performBlur;

		if( doblur ) {
			pingPongBuffer = new PingPongBuffer( w, h, PostProcessor.getFramebufferFormat(), false );
			blur = new Blur( w, h );
			blur.setPasses( 1 );
			blur.setAmount( 1f );
			// blur.setType( BlurType.Gaussian3x3b ); // high defocus
			blur.setType( BlurType.Gaussian3x3 );	// modern machines defocus
		} else {
			buffer = new FrameBuffer( PostProcessor.getFramebufferFormat(), w, h, false );
		}

		combine = new Combine();
		combine.setParam( Combine.Param.Source1Intensity, barrelDistortion ? 0f : 0.15f );
		combine.setParam( Combine.Param.Source2Intensity, barrelDistortion ? 1.2f : 1.1f );
		combine.setParam( Combine.Param.Source1Saturation, 1f );
		combine.setParam( Combine.Param.Source2Saturation, 0.8f );

		crt = new CrtScreen( barrelDistortion );
		crt.setResolution( w, h );
	}

	@Override
	public void dispose() {
		crt.dispose();
		combine.dispose();
		if( doblur ) {
			blur.dispose();
		}

		if( buffer != null ) {
			buffer.dispose();
		}

		if( pingPongBuffer != null ) {
			pingPongBuffer.dispose();
		}
	}

	public void setTime( float time ) {
		crt.setTime( time );
	}

	public void setResolution( float width, float height ) {
		crt.setResolution( width, height );
	}

	public void setOffset( float offset ) {
		crt.setOffset( offset );
	}

	public void setTint( Color tint ) {
		crt.setTint( tint );
	}

	public void setTint( float r, float g, float b ) {
		tmptint.set( r, g, b, 1f );
		crt.setTint( tmptint );
	}

	public void setDistortion( float distortion ) {
		crt.setDistortion( distortion );
	}

	public void setZoom( float zoom ) {
		crt.setZoom( zoom );
	}

	@Override
	public void rebind() {
		crt.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		Texture texsrc = src.getColorBufferTexture();

		Gdx.gl.glDisable( GL10.GL_BLEND );
		Gdx.gl.glDisable( GL10.GL_DEPTH_TEST );
		Gdx.gl.glDepthMask( false );

		if( doblur ) {

			pingPongBuffer.begin();
			{
				// crt pass
				crt.setInput( texsrc ).setOutput( pingPongBuffer.getSourceBuffer() ).render();
				blur.render( pingPongBuffer );
			}
			pingPongBuffer.end();
			combine.setOutput( dest ).setInput( texsrc, pingPongBuffer.getResultTexture() ).render();
		} else {
			// crt pass
			crt.setInput( texsrc ).setOutput( buffer ).render();

			// do combine pass
			combine.setOutput( dest ).setInput( texsrc, buffer.getColorBufferTexture() ).render();
		}
	};

}
