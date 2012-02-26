package com.bitfire.uracer.effects.postprocessing.bloom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.ShaderLoader;

/**
 * @author kalle_h
 *
 */
public class Bloom
{

	/**
	 * To use implement bloom more like a glow. Texture alpha channel can be used as mask which part are glowing and whic are not.
	 * see more info at: http://www.gamasutra.com/view/feature/2107/realtime_glow.php
	 *
	 * NOTE: need to be set before bloom instance is created. After that this does nothing.
	 */
	public static boolean useAlphaChannelAsMask = false;

	/** how many blur pass */
	public int blurPasses = 1;

	private ShaderProgram tresholdShader;
	private ShaderProgram bloomShader;

	private Mesh fullScreenQuad;

	private Texture pingPongTex1;
	private Texture pingPongTex2;
	private Texture original;

	private FrameBuffer frameBuffer;
	private FrameBuffer pingPongBuffer1;
	private FrameBuffer pingPongBuffer2;

	private ShaderProgram blurShader;

	private float bloomIntensity;
	private float originalIntensity;
	private float treshold;
	private int w;
	private int h;
	private boolean blending = false;
	private boolean capturing = false;
	private float r = 0f;
	private float g = 0f;
	private float b = 0f;
	private float a = 0f;
	private boolean disposeFBO = true;

	/**
	 * IMPORTANT NOTE CALL THIS WHEN RESUMING
	 */
	public void resume()
	{
		setSize( w, h );
		setTreshold( treshold );
		setBloomIntesity( bloomIntensity );
		setOriginalIntesity( originalIntensity );

		original = frameBuffer.getColorBufferTexture();
		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();
	}

	/**
	 * Initialize bloom class that capsulate original scene capturate, tresholding, gaussian blurring and blending. Default
	 * values: depth = true blending = false 32bits = true
	 */
	public Bloom()
	{
		initialize( Gdx.graphics.getWidth() / 4, Gdx.graphics.getHeight() / 4, null, true, false, true );
	}

	/**
	 * Initialize bloom class that capsulate original scene capturate, tresholding, gaussian blurring and blending.
	 *
	 * @param FBO_W
	 * @param FBO_H
	 *            how big fbo is used for bloom texture, smaller = more blur and lot faster but aliasing can be problem
	 * @param hasDepth
	 *            do rendering need depth buffer
	 * @param useBlending
	 *            does fbo need alpha channel and is blending enabled when final image is rendered. This allow to combine
	 *            background graphics and only do blooming on certain objects
	 * @param use32bitFBO
	 *            does fbo use higher precision than 16bits.
	 */
	public Bloom( int FBO_W, int FBO_H, boolean hasDepth, boolean useBlending, boolean use32bitFBO )
	{
		initialize( FBO_W, FBO_H, null, hasDepth, useBlending, use32bitFBO );

	}

	/**
	 * EXPERT FUNCTIONALITY. no error checking. Use this only if you know what you are doing. Remember that bloom.capture() clear
	 * the screen so use continue instead if that is a problem.
	 *
	 * Initialize bloom class that capsulate original scene capturate, tresholding, gaussian blurring and blending.
	 *
	 * * @param sceneIsCapturedHere diposing is user responsibility.
	 *
	 * @param FBO_W
	 * @param FBO_H
	 *            how big fbo is used for bloom texture, smaller = more blur and lot faster but aliasing can be problem
	 * @param hasDepth
	 *            do rendering need depth buffer
	 * @param useBlending
	 *            does fbo need alpha channel and is blending enabled when final image is rendered. This allow to combine
	 *            background graphics and only do blooming on certain objects
	 * @param use32bitFBO
	 *            does fbo use higher precision than 16bits.
	 */
	public Bloom( int FBO_W, int FBO_H, FrameBuffer sceneIsCapturedHere, boolean useBlending, boolean use32bitFBO )
	{

		initialize( FBO_W, FBO_H, sceneIsCapturedHere, false, useBlending, use32bitFBO );
		disposeFBO = false;
	}

	private void initialize( int FBO_W, int FBO_H, FrameBuffer fbo, boolean hasDepth, boolean useBlending, boolean use32bitFBO )
	{
		blending = useBlending;
		Format format = null;

		if( use32bitFBO )
		{
			if( useBlending )
			{
				format = Format.RGBA8888;
			} else
			{
				format = Format.RGB888;
			}

		} else
		{
			if( useBlending )
			{
				format = Format.RGBA4444;
			} else
			{
				format = Format.RGB565;
			}
		}
		if( fbo == null )
		{
			frameBuffer = new FrameBuffer( format, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), hasDepth );
		} else
		{
			frameBuffer = fbo;
		}

		pingPongBuffer1 = new FrameBuffer( format, FBO_W, FBO_H, false );
		pingPongBuffer2 = new FrameBuffer( format, FBO_W, FBO_H, false );

		original = frameBuffer.getColorBufferTexture();
		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();

		fullScreenQuad = createFullScreenQuad();
		bloomShader = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom" );

		if( useAlphaChannelAsMask )
		{
			tresholdShader = ShaderLoader.createShader( "bloom/screenspace", "bloom/maskedtreshold" );
		} else
		{
			tresholdShader = ShaderLoader.createShader( "bloom/screenspace", "bloom/treshold" );
		}

		blurShader = ShaderLoader.createShader( "bloom/blurspace", "bloom/gaussian" );

		setSize( FBO_W, FBO_H );
		setBloomIntesity( 1.3f );
		setOriginalIntesity( 0.8f );
		setTreshold( 0.277f );
	}

	public void setClearColor( float r, float g, float b, float a )
	{
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	/**
	 * Call this before rendering scene.
	 */
	public void capture()
	{
		if( !capturing )
		{
			capturing = true;
			frameBuffer.begin();
			Gdx.gl.glClearColor( r, g, b, a );
			Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		}
	}

	/**
	 * Pause capturing to fbo.
	 */
	public void capturePause()
	{
		if( capturing )
		{
			capturing = false;
			frameBuffer.end();
		}
	}

	/** Start capturing again after pause, no clearing is done to framebuffer */
	public void captureContinue()
	{
		if( !capturing )
		{
			capturing = true;
			frameBuffer.begin();
		}
	}

	/**
	 * Call this after scene. Renders the bloomed scene.
	 */
	public void render()
	{
		if( capturing )
		{
			capturing = false;
			frameBuffer.end();
		}

		Gdx.gl.glDisable( GL10.GL_BLEND );
		Gdx.gl.glDisable( GL10.GL_DEPTH_TEST );
		Gdx.gl.glDepthMask( false );

		gaussianBlur();

		if( blending )
		{
			Gdx.gl.glEnable( GL10.GL_BLEND );
			Gdx.gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		}

		original.bind( 0 );
		pingPongTex1.bind( 1 );
		bloomShader.begin();
		{
			bloomShader.setUniformi( "u_texture0", 0 );
			bloomShader.setUniformi( "u_texture1", 1 );
			fullScreenQuad.render( bloomShader, GL20.GL_TRIANGLE_FAN );
		}
		bloomShader.end();
	}

	private void gaussianBlur()
	{
		// cut bright areas of the picture and blit to smaller fbo

		original.bind( 0 );
		pingPongBuffer1.begin();
		{
			tresholdShader.begin();
			{
				tresholdShader.setUniformi( "u_texture0", 0 );
				fullScreenQuad.render( tresholdShader, GL20.GL_TRIANGLE_FAN, 0, 4 );
			}
			tresholdShader.end();
		}
		pingPongBuffer1.end();

		for( int i = 0; i < blurPasses; i++ )
		{
			pingPongTex1.bind( 0 );

			// horizontal
			pingPongBuffer2.begin();
			{
				blurShader.begin();
				{
					blurShader.setUniformi( "u_texture", 0 );
					blurShader.setUniformf( "dir", 1f, 0f );
					fullScreenQuad.render( blurShader, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				blurShader.end();
			}
			pingPongBuffer2.end();

			pingPongTex2.bind( 0 );
			// vertical
			pingPongBuffer1.begin();
			{
				blurShader.begin();
				{
					blurShader.setUniformi( "u_texture", 0 );
					blurShader.setUniformf( "dir", 0f, 1f );

					fullScreenQuad.render( blurShader, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				blurShader.end();
			}
			pingPongBuffer1.end();
		}
	}

	public void setBloomIntesity( float intensity )
	{
		bloomIntensity = intensity;
		bloomShader.begin();
		{
			bloomShader.setUniformf( "BloomIntensity", intensity );
		}
		bloomShader.end();
	}

	public void setOriginalIntesity( float intensity )
	{
		originalIntensity = intensity;
		bloomShader.begin();
		{
			bloomShader.setUniformf( "OriginalIntensity", intensity );
		}
		bloomShader.end();
	}

	public void setTreshold( float treshold )
	{
		this.treshold = treshold;
		tresholdShader.begin();
		{
			tresholdShader.setUniformf( "treshold", treshold );
			tresholdShader.setUniformf( "tresholdD", (1f / treshold) );
		}
		tresholdShader.end();
	}

	private void setSize( int FBO_W, int FBO_H )
	{
		w = FBO_W;
		h = FBO_H;
		blurShader.begin();
		blurShader.setUniformf( "size", FBO_W, FBO_H );
		blurShader.end();
	}

	/**
	 * Call this when application is exiting.
	 *
	 */
	public void dispose()
	{
		if( disposeFBO ) frameBuffer.dispose();

		fullScreenQuad.dispose();

		pingPongBuffer1.dispose();
		pingPongBuffer2.dispose();

		blurShader.dispose();
		bloomShader.dispose();
		tresholdShader.dispose();
	}

	private Mesh createFullScreenQuad()
	{
		float[] verts = new float[ 16 ];// VERT_SIZE
		int i = 0;
		verts[i++] = -1; // x1
		verts[i++] = -1; // y1

		verts[i++] = 0f; // u1
		verts[i++] = 0f; // v1

		verts[i++] = 1f; // x2
		verts[i++] = -1; // y2

		verts[i++] = 1f; // u2
		verts[i++] = 0f; // v2

		verts[i++] = 1f; // x3
		verts[i++] = 1f; // y2

		verts[i++] = 1f; // u3
		verts[i++] = 1f; // v3

		verts[i++] = -1; // x4
		verts[i++] = 1f; // y4

		verts[i++] = 0f; // u4
		verts[i++] = 1f; // v4

		Mesh tmpMesh = new Mesh( true, 4, 0,
			new VertexAttribute(Usage.Position, 2, "a_position"),
			new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0") );

		tmpMesh.setVertices( verts );
		return tmpMesh;
	}

}
