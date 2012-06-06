package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.TvLines;

public class Tv extends PostProcessorEffect {
	// private FrameBuffer quadBuffer;
	private TvLines tvlines;
	private Color tmptint = new Color();

	// private Copy copy;

	public Tv() {
		// quadBuffer = new FrameBuffer( PostProcessor.getFramebufferFormat(), Config.PostProcessing.PotRttFboWidth,
		// Config.PostProcessing.PotRttFboHeight, false );
		// quadBuffer = new FrameBuffer( PostProcessor.getFramebufferFormat(), Gdx.graphics.getWidth(),
		// Gdx.graphics.getHeight(), false );
		tvlines = new TvLines();
		// copy = new Copy();
	}

	@Override
	public void dispose() {
		// copy.dispose();
		tvlines.dispose();
		// quadBuffer.dispose();
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
		// Texture texsrc = src.getColorBufferTexture();
		//
		// copy.setInput( texsrc ).setOutput( quadBuffer ).render();
		// tvlines.setOutput( dest ).setInput( quadBuffer.getColorBufferTexture() ).render();

		tvlines.setInput( src ).setOutput( dest ).render();
	};

}
