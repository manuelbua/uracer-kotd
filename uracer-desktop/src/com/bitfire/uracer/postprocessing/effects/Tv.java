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
import com.bitfire.uracer.postprocessing.filters.TvLines;

public class Tv extends PostProcessorEffect {
	private PingPongBuffer pingPongBuffer;
	private TvLines tvlines;
	private Color tmptint = new Color();
	private Blur blur;
	private Combine combine;

	public Tv() {
		// the effect is designed to work on the whole screen area, no small/mid size tricks!
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();

		pingPongBuffer = new PingPongBuffer( w, h, PostProcessor.getFramebufferFormat(), false );

		blur = new Blur( w, h );
		blur.setPasses( 1 );
		blur.setAmount( 1f );
		blur.setType( BlurType.Gaussian3x3b );

		combine = new Combine();
		combine.setParam( Combine.Param.Source1Intensity, 0.15f );
		combine.setParam( Combine.Param.Source2Intensity, 1.1f );
		combine.setParam( Combine.Param.Source1Saturation, 1f );
		combine.setParam( Combine.Param.Source2Saturation, 1f );

		tvlines = new TvLines();
		tvlines.setResolution( w, h );
	}

	@Override
	public void dispose() {
		tvlines.dispose();
		combine.dispose();
		blur.dispose();
		pingPongBuffer.dispose();
	}

	public void setTime( float time ) {
		tvlines.setTime( time );
	}

	public void setResolution( float width, float height ) {
		tvlines.setResolution( width, height );
	}

	public void setOffset( float offset ) {
		tvlines.setOffset( offset );
	}

	public void setTint( Color tint ) {
		tvlines.setTint( tint );
	}

	public void setTint( float r, float g, float b ) {
		tmptint.set( r, g, b, 1f );
		tvlines.setTint( tmptint );
	}

	@Override
	public void rebind() {
		tvlines.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		Texture texsrc = src.getColorBufferTexture();

		Gdx.gl.glDisable( GL10.GL_BLEND );
		Gdx.gl.glDisable( GL10.GL_DEPTH_TEST );
		Gdx.gl.glDepthMask( false );

		pingPongBuffer.begin();
		{
			// do tv lines
			tvlines.setInput( texsrc ).setOutput( pingPongBuffer.getSourceBuffer() ).render();

			// blur pass
			blur.render( pingPongBuffer );
		}
		pingPongBuffer.end();

		// combine original + blurred tv-lines
		combine.setOutput( dest ).setInput( texsrc, pingPongBuffer.getResultTexture() ).render();
	};

}
