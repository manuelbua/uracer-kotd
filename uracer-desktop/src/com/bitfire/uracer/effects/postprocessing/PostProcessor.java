package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class PostProcessor
{
	private FrameBuffer fbScene;
	private Format fbFormat;
	private Mesh fullScreenQuad;
	private boolean capturing = false;
	private IPostProcessorEffect effect = null;

	private Texture texScene;

	public PostProcessor( int fboWidth, int fboHeight, boolean useDepth, boolean useAlphaChannel, boolean use32Bits)
	{
		if( use32Bits )
		{
			if( useAlphaChannel )
			{
				fbFormat = Format.RGBA8888;
			} else
			{
				fbFormat = Format.RGB888;
			}

		} else
		{
			if( useAlphaChannel )
			{
				fbFormat = Format.RGBA4444;
			} else
			{
				fbFormat = Format.RGB565;
			}
		}

		fbScene = new FrameBuffer( fbFormat, fboWidth, fboHeight, useDepth );
		texScene = fbScene.getColorBufferTexture();
		fullScreenQuad = createFullscreenQuad();
		capturing = false;
	}

	public void dispose()
	{
		if(effect != null)
			effect.dispose();

		fbScene.dispose();
		fullScreenQuad.dispose();
	}

	public void setEffect(IPostProcessorEffect effect)
	{
		this.effect = effect;
	}

	public Format getFramebufferFormat()
	{
		return fbFormat;
	}

	/**
	 * Start capturing the scene
	 */
	public void capture()
	{
		if(!capturing && ( effect != null ))
		{
			capturing = true;
			fbScene.begin();

			Color c = Color.CLEAR;
			if(effect != null)
				c = effect.getClearColor();

			Gdx.gl.glClearColor( c.r, c.g, c.b, c.a );
			Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		}
	}

	/**
	 * Pause capturing
	 */
	public void capturePause()
	{
		if(capturing)
		{
			capturing = false;
			fbScene.end();
		}
	}

	/**
	 * Start capturing again, after pause
	 */
	public void captureContinue()
	{
		if(!capturing)
		{
			capturing = true;
			fbScene.begin();
		}
	}

	/**
	 * call this when resuming
	 */
	public void resume()
	{
		texScene = fbScene.getColorBufferTexture();
		if( effect != null )
			effect.resume();
	}

	/**
	 * Finish capturing the scene, post-process and render the effect, if any
	 */
	public void render()
	{
		if(capturing && ( effect != null ))
		{
			capturing = false;
			fbScene.end();

			if(effect != null)
			{
				effect.render( fullScreenQuad, texScene );
			}
		}
	}

	private Mesh createFullscreenQuad() {
		// vertex coord
		verts[X1] = -1;
		verts[Y1] = -1;

		verts[X2] = 1;
		verts[Y2] = -1;

		verts[X3] = 1;
		verts[Y3] = 1;

		verts[X4] = -1;
		verts[Y4] = 1;

		// tex coords
		verts[U1] = 0f;
		verts[V1] = 0f;

		verts[U2] = 1f;
		verts[V2] = 0f;

		verts[U3] = 1f;
		verts[V3] = 1f;

		verts[U4] = 0f;
		verts[V4] = 1f;

		Mesh tmpMesh = new Mesh(true, 4, 0, new VertexAttribute(
				Usage.Position, 2, "a_position"), new VertexAttribute(
				Usage.TextureCoordinates, 2, "a_texCoord0"));

		tmpMesh.setVertices(verts);
		return tmpMesh;

	}

	private static final int VERT_SIZE = 16;
	private static float[] verts = new float[VERT_SIZE];
	private static final int X1 = 0;
	private static final int Y1 = 1;
	private static final int U1 = 2;
	private static final int V1 = 3;
	private static final int X2 = 4;
	private static final int Y2 = 5;
	private static final int U2 = 6;
	private static final int V2 = 7;
	private static final int X3 = 8;
	private static final int Y3 = 9;
	private static final int U3 = 10;
	private static final int V3 = 11;
	private static final int X4 = 12;
	private static final int Y4 = 13;
	private static final int U4 = 14;
	private static final int V4 = 15;
}
