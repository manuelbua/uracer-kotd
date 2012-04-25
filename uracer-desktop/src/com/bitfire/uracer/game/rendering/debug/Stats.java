package com.bitfire.uracer.game.rendering.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bitfire.uracer.URacer;

public class Stats {
	private int PanelWidth;
	private int PanelHeight;

	// graphics data
	private Pixmap pixels;
	private Texture texture;
	private TextureRegion region;

	// timing data
	private long startTime;
	private long intervalNs;

	// stats data
	private float[] dataRenderTime;
	private float[] dataFps;
	private float[] dataPhysicsTime;

	// private float[] dataTimeAliasing;

	public Stats() {
		init( 100, 50, 0.2f );
	}

	public Stats( int width, int height, float updateHz ) {
		init( width, height, updateHz );
	}

	public Stats( float updateHz ) {
		init( 100, 50, updateHz );
	}

	private void init( int width, int height, float updateHz ) {
		assert (width < 256 && height < 256);

		PanelWidth = width;
		PanelHeight = height;
		intervalNs = (long)(1000000000L * (1f/updateHz));

		pixels = new Pixmap( PanelWidth, PanelHeight, Format.RGBA8888 );
		texture = new Texture( 256, 256, Format.RGBA8888 );
		texture.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		region = new TextureRegion( texture, 0, 0, pixels.getWidth(), pixels.getHeight() );

		// create data
		dataRenderTime = new float[ PanelWidth ];
		dataFps = new float[ PanelWidth ];
		dataPhysicsTime = new float[ PanelWidth ];
		// dataTimeAliasing = new float[ PanelWidth ];

		reset();
	}

	public void dispose() {
		pixels.dispose();
		texture.dispose();
	}

	private void reset() {
		for( int i = 0; i < PanelWidth; i++ ) {
			dataRenderTime[i] = 0;
			dataPhysicsTime[i] = 0;
			dataFps[i] = 0;
			// dataTimeAliasing[i] = 0;
		}

		plot();
		startTime = System.nanoTime();
	}

	public TextureRegion getRegion() {
		return region;
	}

	public int getWidth() {
		return PanelWidth;
	}

	public int getHeight() {
		return PanelHeight;
	}

	public void update() {
		if( collect() ) {
			plot();
		}
	}

	private void plot() {
		// background
		pixels.setColor( 0, 0, 0, 0.8f );
		pixels.fill();

		float oneOn60 = 1f / 60f;
		float ratio_rtime = (PanelHeight / 2) / oneOn60;
		float ratio_ptime = (PanelHeight / 2) / oneOn60;
		float ratio_fps = (PanelHeight / 2) * oneOn60;

		float alpha = 0.5f;
		for( int x = 0; x < PanelWidth; x++ ) {
			int xc = PanelWidth - x - 1;
			int value = 0;

			// render time
			value = (int)(dataRenderTime[x] * ratio_rtime);
			if( value > 0 ) {
				pixels.setColor( 0, 0.5f, 1f, alpha );
				pixels.drawLine( xc, 0, xc, value );
			}

			// physics time
			value = (int)(dataPhysicsTime[x] * ratio_ptime);
			pixels.setColor( 1, 1, 1, alpha );
			pixels.drawLine( xc, 0, xc, value );

			// fps
			value = (int)(dataFps[x] * ratio_fps);
			if( value > 0 ) {
				pixels.setColor( 0, 1, 1, .8f );
				pixels.drawPixel( xc, value );
			}

			// time aliasing
			// value = (int)( AMath.clamp(dataTimeAliasing[x] * PanelHeight, 0, PanelHeight) );
			// if( value > 0 )
			// {
			// pixels.setColor( 1, 0, 1, .8f );
			// pixels.drawPixel( xc, value );
			// }
		}

		texture.draw( pixels, 0, 0 );
	}

	private boolean collect() {
		long time = System.nanoTime();

		if( time - startTime > intervalNs ) {
			// shift values
			for( int i = PanelWidth - 1; i > 0; i-- ) {
				dataRenderTime[i] = dataRenderTime[i - 1];
				dataPhysicsTime[i] = dataPhysicsTime[i - 1];
				dataFps[i] = dataFps[i - 1];
				// dataTimeAliasing[i] = dataTimeAliasing[i-1];
			}

			dataPhysicsTime[0] = URacer.getPhysicsTime();
			dataRenderTime[0] = URacer.getRenderTime();
			dataFps[0] = Gdx.graphics.getFramesPerSecond();
			// dataTimeAliasing[0] = URacer.getTemporalAliasing();

			startTime = time;

			return true;
		}

		return false;
	}
}
