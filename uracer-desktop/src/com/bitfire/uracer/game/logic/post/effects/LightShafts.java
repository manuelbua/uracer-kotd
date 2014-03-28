
package com.bitfire.uracer.game.logic.post.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.postprocessing.PostProcessorEffect;
import com.bitfire.postprocessing.filters.Blur;
import com.bitfire.postprocessing.filters.Blur.BlurType;
import com.bitfire.postprocessing.filters.Combine;
import com.bitfire.postprocessing.filters.Threshold;
import com.bitfire.postprocessing.utils.FullscreenQuad;
import com.bitfire.postprocessing.utils.PingPongBuffer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.utils.ShaderLoader;

public class LightShafts extends PostProcessorEffect {
	public enum Quality {
		High(1), Medium(0.75f), Low(0.5f);
		public final float scale;

		Quality (float scale) {
			this.scale = scale;
		}
	}

	private final PingPongBuffer occlusionMap;
	private final ShaderProgram shShafts;
	private final FullscreenQuad quad = new FullscreenQuad();
	private Blur blur;
	private Combine combine;
	private Threshold threshold;
	private float oneOnW, oneOnH;

	private GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			debug(GameEvents.gameRenderer.batch);
		}
	};

	public LightShafts (int fboWidth, int fboHeight, Quality quality) {
		Gdx.app.log("LightShafts", "Quality profile = " + quality.toString());
		float oscale = quality.scale;

		oneOnW = 1f / (float)Config.Graphics.ReferenceScreenWidth;
		oneOnH = 1f / (float)Config.Graphics.ReferenceScreenHeight;

		// maps
		occlusionMap = new PingPongBuffer((int)((float)fboWidth * oscale), (int)((float)fboHeight * oscale), Format.RGBA8888, false);

		// shaders
		shShafts = ShaderLoader.fromFile("screenspace", "lightshafts/lightshafts");
		combine = new Combine();
		threshold = new Threshold();

		// blur
		blur = new Blur(occlusionMap.width, occlusionMap.height);
		blur.setType(BlurType.Gaussian5x5b);

		blur.setPasses(2);
		int w = Gdx.graphics.getWidth();
		if (w >= 1920)
			blur.setPasses(4);
		else if (w >= 1680)
			blur.setPasses(3);
		else if (w >= 1280) blur.setPasses(2);

		setParams(16, 0.0034f, 1f, 0.84f, 5.65f, 1f, Config.Graphics.ReferenceScreenWidth / 2,
			Config.Graphics.ReferenceScreenHeight / 2);

		// enableDebug();
	}

	@Override
	public void dispose () {
		disableDebug();
		blur.dispose();
		shShafts.dispose();
		combine.dispose();
		threshold.dispose();
		occlusionMap.dispose();
	}

	public void enableDebug () {
		GameEvents.gameRenderer.addListener(gameRendererEvent, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT);
	}

	public void disableDebug () {
		GameEvents.gameRenderer.removeListener(gameRendererEvent, GameRendererEvent.Type.BatchDebug,
			GameRendererEvent.Order.DEFAULT);
	}

	private void dbgTextureW (SpriteBatch batch, float width, Texture tex, int index) {
		if (tex == null) return;

		float h = width / ScaleUtils.RefAspect;
		float x = Config.Graphics.ReferenceScreenWidth - width - 10;
		float y = index * 10;
		batch.draw(tex, x, y, width, h);
	}

	private void debug (SpriteBatch batch) {
		Gdx.gl.glDisable(GL20.GL_BLEND);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		batch.disableBlending();
		dbgTextureW(batch, 360, occlusionMap.getResultTexture(), 50);
	}

	private float[] vLightPos = new float[2];

	public void setParams (int samples, float exposure, float decay, float density, float weight, float illuminationDecay,
		float lightScreenPosX, float lightScreenPosY) {
		shShafts.begin();
		shShafts.setUniformi("samples", samples); // 16
		shShafts.setUniformf("exposure", exposure); // 0.0034
		shShafts.setUniformf("decay", decay); // 1
		shShafts.setUniformf("density", density); // 0.84
		shShafts.setUniformf("weight", weight); // 5.65
		shShafts.setUniformf("illuminationDecay", illuminationDecay); // 1
		// normalized position
		vLightPos[0] = lightScreenPosX * oneOnW;
		vLightPos[1] = 1 - lightScreenPosY * oneOnH;
		shShafts.setUniform2fv("lightPositionOnScreen", vLightPos, 0, 2);
		shShafts.end();
	}

	public void setLightScreenPosition (float x, float y) {
		setLightScreenPositionN(x * oneOnW, y * oneOnH);
	}

	public void setLightScreenPositionN (float x, float y) {
		vLightPos[0] = x;
		vLightPos[1] = 1 - y;
		shShafts.begin();
		shShafts.setUniform2fv("lightPositionOnScreen", vLightPos, 0, 2);
		shShafts.end();
	}

	public void setSamples (int samples) {
		shShafts.begin();
		shShafts.setUniformi("samples", samples); // 16
		shShafts.end();
	}

	public void setExposure (float exposure) {
		shShafts.begin();
		shShafts.setUniformf("exposure", exposure); // 0.0034
		shShafts.end();
	}

	public void setDecay (float decay) {
		shShafts.begin();
		shShafts.setUniformf("decay", decay); // 1
		shShafts.end();
	}

	public void setDensity (float density) {
		shShafts.begin();
		shShafts.setUniformf("density", density); // 0.84
		shShafts.end();
	}

	public void setWeight (float weight) {
		shShafts.begin();
		shShafts.setUniformf("weight", weight); // 5.65
		shShafts.end();
	}

	public void setIlluminationDecay (float illuminationDecay) {
		shShafts.begin();
		shShafts.setUniformf("illuminationDecay", illuminationDecay); // 1
		shShafts.end();
	}

	public void setThreshold (float gamma) {
		this.threshold.setTreshold(gamma);
	}

	public Combine getCombinePass () {
		return combine;
	}

	@Override
	public void render (FrameBuffer src, FrameBuffer dest) {
		Texture tsrc = src.getColorBufferTexture();

		// blur.setPasses(2);

		// 1, render occlusion map
		occlusionMap.begin();
		{
			threshold.setInput(tsrc).setOutput(occlusionMap.getSourceBuffer()).render();
			blur.render(occlusionMap);
		}
		occlusionMap.end();
		// threshold.setInput(tsrc).setOutput(occlusionMap.getResultBuffer()).render(); // threshold without blur

		Texture result = occlusionMap.getResultTexture();

		// 2, render shafts
		occlusionMap.capture();
		{
			shShafts.begin();
			{
				// Gdx.gl.glClearColor(0, 0, 0, 1);
				// Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				result.bind(0);
				shShafts.setUniformi("u_texture", 0);
				quad.render(shShafts);
			}
			shShafts.end();

			// blur pass
			// blur.render(occlusionMap);
		}
		occlusionMap.end();

		restoreViewport(dest);

		// 3, combine
		combine.setOutput(dest).setInput(tsrc, occlusionMap.getResultTexture()).render();
	}

	@Override
	public void rebind () {
	}
}
