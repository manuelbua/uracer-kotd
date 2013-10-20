
package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.utils.AMath;

public final class DebugStatistics {
	// public statistical data
	public WindowedMean meanPhysics = new WindowedMean(16);
	public WindowedMean meanRender = new WindowedMean(16);
	public WindowedMean meanTickCount = new WindowedMean(16);

	// internal data for graphics representation
	private Pixmap pixels;
	private Texture texture;
	private TextureRegion region;
	private int PanelWidth;
	private int PanelHeight;
	private float ratio_rtime, ratio_ptime, ratio_fps;

	// internal timing data
	private long startTime;
	private long intervalNs;

	// internal stats data
	private float[] dataRenderTime;
	private float[] dataFps;
	private float[] dataPhysicsTime;
	private float[] dataTimeAliasing;

	public DebugStatistics () {
		init(100, 50, 0.2f);
	}

	public DebugStatistics (int width, int height, float updateHz) {
		init(width, height, updateHz);
	}

	public DebugStatistics (float updateHz) {
		init(100, 50, updateHz);
	}

	private void init (int width, int height, float updateHz) {
		// assert (width < 256 && height < 256);

		final float oneOnUpdHz = 1f / updateHz;

		PanelWidth = width;
		PanelHeight = height;
		intervalNs = (long)(1000000000L * oneOnUpdHz);

		pixels = new Pixmap(PanelWidth, PanelHeight, Format.RGBA8888);
		texture = new Texture(width, height, Format.RGBA8888);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		region = new TextureRegion(texture, 0, 0, pixels.getWidth(), pixels.getHeight());

		// create data
		dataRenderTime = new float[PanelWidth];
		dataFps = new float[PanelWidth];
		dataPhysicsTime = new float[PanelWidth];
		dataTimeAliasing = new float[PanelWidth];

		// precompute constants
		ratio_rtime = ((float)PanelHeight / 2f) * Config.Physics.TimestepHz;
		ratio_ptime = ((float)PanelHeight / 2f) * Config.Physics.TimestepHz;
		ratio_fps = ((float)PanelHeight / 2f) * oneOnUpdHz;

		reset();
	}

	public void dispose () {
		pixels.dispose();
		texture.dispose();
	}

	private void reset () {
		for (int i = 0; i < PanelWidth; i++) {
			dataRenderTime[i] = 0;
			dataPhysicsTime[i] = 0;
			dataFps[i] = 0;
			dataTimeAliasing[i] = 0;
		}

		plot();
		startTime = TimeUtils.nanoTime();
	}

	public TextureRegion getRegion () {
		return region;
	}

	public int getWidth () {
		return PanelWidth;
	}

	public int getHeight () {
		return PanelHeight;
	}

	public void update () {
		if (collect()) {
			plot();
		}
	}

	private void plot () {
		// background
		pixels.setColor(0, 0, 0, 0.8f);
		pixels.fill();

		float alpha = 0.5f;
		for (int x = 0; x < PanelWidth; x++) {
			int xc = PanelWidth - x - 1;
			int value = 0;

			// render time
			value = (int)(dataRenderTime[x] * ratio_rtime);
			if (value > 0) {
				pixels.setColor(0, 0.5f, 1f, alpha);
				pixels.drawLine(xc, 0, xc, value);
			}

			// physics time
			value = (int)(dataPhysicsTime[x] * ratio_ptime);
			pixels.setColor(1, 1, 1, alpha);
			pixels.drawLine(xc, 0, xc, value);

			// fps
			value = (int)(dataFps[x] * ratio_fps);
			if (value > 0) {
				pixels.setColor(0, 1, 1, .8f);
				pixels.drawPixel(xc, value);
			}

			// time aliasing
			value = (int)(AMath.clamp(dataTimeAliasing[x] * PanelHeight, 0, PanelHeight));
			if (value > 0) {
				pixels.setColor(1, 0, 1, .8f);
				pixels.drawPixel(xc, value);
			}
		}

		texture.draw(pixels, 0, 0);
	}

	private boolean collect () {
		long time = TimeUtils.nanoTime();

		if (time - startTime > intervalNs) {
			// shift values
			for (int i = PanelWidth - 1; i > 0; i--) {
				dataRenderTime[i] = dataRenderTime[i - 1];
				dataPhysicsTime[i] = dataPhysicsTime[i - 1];
				dataFps[i] = dataFps[i - 1];
				dataTimeAliasing[i] = dataTimeAliasing[i - 1];
			}

			meanPhysics.addValue(URacer.Game.getPhysicsTime());
			meanRender.addValue(URacer.Game.getRenderTime());
			meanTickCount.addValue(URacer.Game.getLastTicksCount());

			dataPhysicsTime[0] = meanPhysics.getMean();
			dataRenderTime[0] = meanRender.getMean();
			dataFps[0] = Gdx.graphics.getFramesPerSecond();
			dataTimeAliasing[0] = URacer.Game.getTemporalAliasing();

			startTime = time;
			return true;
		}

		return false;
	}
}
