package com.bitfire.uracer.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

public class GameBatchRenderer {
	private SpriteBatch batch = null;
	private boolean begin = false;
	private final GL20 gl;

	public GameBatchRenderer(GL20 gl) {
		batch = new SpriteBatch( 1000, 8 );
		begin = false;
		this.gl = gl;
	}

	public void dispose() {
		batch.dispose();
	}

	public SpriteBatch begin( Matrix4 proj, Matrix4 viewxform ) {
		if( !begin ) {
			begin = true;
			gl.glActiveTexture( GL20.GL_TEXTURE0 );
			batch.setProjectionMatrix( proj );
			batch.setTransformMatrix( viewxform );
			batch.begin();
			return batch;
		}

		return null;
	}

	public void begin( Camera camera ) {
		begin( camera.projection, camera.view );
	}

	public void end() {
		if( begin ) {
			batch.end();
			begin = false;
		}
	}

}
