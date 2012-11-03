/*******************************************************************************
 * Copyright 2012 bmanuel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.bitfire.uracer.game.logic.post.ssao;

import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.postprocessing.PostProcessorEffect;
import com.bitfire.postprocessing.filters.Blur;
import com.bitfire.postprocessing.utils.FullscreenQuad;
import com.bitfire.postprocessing.utils.PingPongBuffer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.utils.ShaderLoader;

public final class Ssao extends PostProcessorEffect {

	private final PingPongBuffer occlusionMap;
	private Texture normalDepthMap;
	private final ShaderProgram shMix, shSsao;
	private final FullscreenQuad quad = new FullscreenQuad();
	private final Texture randomField;
	private Blur blur;

	Matrix3 mtxRot = new Matrix3();
	Matrix3 invRot = new Matrix3();
	Matrix4 invPrj = new Matrix4();

	public Ssao () {
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		float oscale = Config.PostProcessing.SsaoMapScale;

		// maps
		occlusionMap = new PingPongBuffer((int)(w * oscale), (int)(h * oscale), Format.RGBA8888, false);

		// shaders
		shMix = ShaderLoader.fromFile("screenspace", "ssao/mix");
		shSsao = ShaderLoader.fromFile("ssao/ssao", "ssao/ssao");

		// blur
		blur = new Blur(occlusionMap.width, occlusionMap.height);

		// compute random field for the ssao shader
		int width = 32;
		int height = 32;
		Format format = Format.RGBA8888;
		randomField = new Texture(width, height, format);
		randomField.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		randomField.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

		Pixmap pixels = new Pixmap(width, height, format);
		ByteBuffer bytes = pixels.getPixels();
		int wrote = 0;

		while (wrote < width * height * 4) {
			float x = (MathUtils.random() - 0.5f) * 2.0f;
			float y = (MathUtils.random() - 0.5f) * 2.0f;
			float z = (MathUtils.random() - 0.5f) * 2.0f;
			float l = (float)Math.sqrt(x * x + y * y + z * z);
			if (l <= 1.0f && l > 0.1f) {
				x = (x + 1.0f) * 0.5f;
				y = (y + 1.0f) * 0.5f;
				z = (z + 1.0f) * 0.5f;
				bytes.put((byte)(x * 255f));
				bytes.put((byte)(y * 255f));
				bytes.put((byte)(z * 255f));
				bytes.put((byte)255);
				wrote += 4;
			}
		}

		bytes.flip();
		randomField.draw(pixels, 0, 0);
		pixels.dispose();
	}

	@Override
	public void dispose () {
		randomField.dispose();
		blur.dispose();
		shSsao.dispose();
		shMix.dispose();
		occlusionMap.dispose();
	}

	public void setNormalDepthMap (Texture normalDepthMap) {
		this.normalDepthMap = normalDepthMap;
	}

	@Override
	public void render (final FrameBuffer src, final FrameBuffer dest) {
		Texture tsrc = src.getColorBufferTexture();

		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

		Camera cam = GameEvents.gameRenderer.camPersp;

		mtxRot.set(cam.view);
		invPrj.set(cam.projection).inv();
		invRot.set(mtxRot).inv();

		occlusionMap.begin();
		occlusionMap.capture();
		{
			shSsao.begin();
			{
				Gdx.gl.glClearColor(1, 1, 1, 1);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

				// samplers
				normalDepthMap.bind(0);
				randomField.bind(1);

				shSsao.setUniformi("normaldepth", 0);
				shSsao.setUniformi("random_field", 1);

				shSsao.setUniformMatrix("proj", cam.projection);
				shSsao.setUniformMatrix("inv_proj", invPrj);
				shSsao.setUniformMatrix("inv_rot", invRot);

				// settings to play with
				shSsao.setUniformf("viewport", occlusionMap.width, occlusionMap.height);
				shSsao.setUniformf("near", cam.near);
				shSsao.setUniformf("far", cam.far);
				shSsao.setUniformf("radius", 0.1f);
				shSsao.setUniformf("epsilon", 0.001f);
				shSsao.setUniformf("full_occlusion_treshold", 0.1f);
				shSsao.setUniformf("no_occlusion_treshold", 0.3f);
				shSsao.setUniformf("occlusion_power", 2f);
				shSsao.setUniformf("power", 1f);

				shSsao.setUniformi("sample_count", 9);
				shSsao.setUniformf("pattern_size", 3);

				quad.render(shSsao);
			}
			shSsao.end();

			// blur pass
// blur.setType(BlurType.Gaussian5x5b);
// blur.setPasses(1);
// blur.render(occlusionMap);
		}
		occlusionMap.end();

		if (dest != null) dest.begin();
		shMix.begin();
		{
			tsrc.bind(0);
			occlusionMap.getResultTexture().bind(1);
			// normalDepthMap.bind(2);

			shMix.setUniformi("scene", 1);
			shMix.setUniformi("occlusion_map", 1);

			quad.render(shMix);
		}
		shMix.end();
		if (dest != null) dest.end();
	}

	@Override
	public void rebind () {
	}
}
