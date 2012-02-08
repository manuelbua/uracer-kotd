package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class PostProcessor
{
	private static FrameBuffer frameBuffer;
	private static Mesh quad;
	private static PostProcessEffect effect;

	public static void init( int rttWidth, int rttHeight )
	{
		frameBuffer = new FrameBuffer( Format.RGBA8888, rttWidth, rttHeight, true );
		frameBuffer.getColorBufferTexture().setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		frameBuffer.getColorBufferTexture().setWrap( TextureWrap.ClampToEdge, TextureWrap.ClampToEdge );
		quad = createQuadMesh();
		effect = null;
	}

	public static void begin()
	{
		if( hasEffect() )
		{
			frameBuffer.begin();
		}
	}

	public static void end()
	{
		if( hasEffect() )
		{
			frameBuffer.end();
			render();
		}
	}

	public static boolean hasEffect()
	{
		return (effect != null) && effect.isEnabled();
	}

	public static void setEffect( PostProcessEffect effect )
	{
		PostProcessor.effect = effect;
	}

	public static PostProcessEffect getEffect()
	{
		return PostProcessor.effect;
	}

	private static void render()
	{
		frameBuffer.getColorBufferTexture().bind(0);

		ShaderProgram shader = effect.getShader();
		shader.begin();
		effect.onBeforeShaderPass();
		quad.render( shader, GL20.GL_TRIANGLE_FAN );
		shader.end();
	}

	private static Mesh createQuadMesh() {
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
				Usage.TextureCoordinates, 2, "a_texCoord"));

		tmpMesh.setVertices(verts);
		return tmpMesh;

	}

	static public final int VERT_SIZE = 16;
	private static float[] verts = new float[VERT_SIZE];
	static public final int X1 = 0;
	static public final int Y1 = 1;
	static public final int U1 = 2;
	static public final int V1 = 3;
	static public final int X2 = 4;
	static public final int Y2 = 5;
	static public final int U2 = 6;
	static public final int V2 = 7;
	static public final int X3 = 8;
	static public final int Y3 = 9;
	static public final int U3 = 10;
	static public final int V3 = 11;
	static public final int X4 = 12;
	static public final int Y4 = 13;
	static public final int U4 = 14;
	static public final int V4 = 15;
}
