
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.utils.ScaleUtils;

public class GameBatchRenderer {
	private SpriteBatch batch = null;
	private boolean begin = false;
	private final GL20 gl;
	private Matrix4 topLeftOrigin, identity; // batch

	public GameBatchRenderer (GL20 gl) {
		// setup a top-left origin
		// y-flip
		topLeftOrigin = new Matrix4();
		topLeftOrigin.setToOrtho(0, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, 0, 0, 10);
		identity = new Matrix4();

		// Issues may arise on Tegra2 (Asus Transformer) devices if the buffers'
		// count is higher than 10
		// batch = new SpriteBatch(1000, 8);
		batch = new SpriteBatch();
		begin = false;
		this.gl = gl;
	}

	public void dispose () {
		batch.dispose();
	}

	public SpriteBatch begin (Matrix4 proj, Matrix4 viewxform) {
		if (!begin) {
			begin = true;
			gl.glActiveTexture(GL20.GL_TEXTURE0);
			batch.setProjectionMatrix(proj);
			batch.setTransformMatrix(viewxform);
			batch.begin();
			return batch;
		}

		return null;
	}

	public SpriteBatch begin (Camera camera) {
		return begin(camera.projection, camera.view);
	}

	public SpriteBatch beginTopLeft () {
		return begin(topLeftOrigin, identity);
	}

	public void end () {
		if (begin) {
			batch.end();
			begin = false;
		}
	}

}
