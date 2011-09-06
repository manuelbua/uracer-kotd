package com.bitfire.uracer.tiled;

import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.loaders.g3d.G3dtLoader;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.Convert;

/**
 * The model is expected to follow the z-up convention.
 *
 * @author manuel
 *
 */
public class OrthographicAlignedMesh
{
//	private Mesh mesh;
	private UStillModel model;
	private StillModel model_workaround;	// FIXME, this is pure shit...
	private Material material;
	private Texture texture;
	private TextureAttribute textureAttribute;

	// matrix state
	private Matrix4 mtx_model = new Matrix4();
	private Matrix4 mtx_combined = new Matrix4();

	private static ShaderProgram shaderProgram = null;

	// scale
	private float scale, scalingFactor;
	private Vector3 scaleAxis = new Vector3();

	// position
	private Vector2 positionOffsetPx = new Vector2( 0, 0 );
	private Vector2 positionPx = new Vector2();

	// temporaries
	private Vector3 tmp_vec = new Vector3();
	private Matrix4 tmp_mtx = new Matrix4();


	// explicitle initialize the static iShader member
	// (Android: statics need to be re-initialized!)
	public static void initialize()
	{
		String vertexShader =
				"uniform mat4 u_mvpMatrix;					\n" +
				"attribute vec4 a_position;					\n" +
				"attribute vec2 a_texCoord0;				\n" +
				"varying vec2 v_TexCoord;					\n" +
				"void main()								\n" +
				"{											\n" +
				"	gl_Position = u_mvpMatrix * a_position;	\n" +
				"	v_TexCoord = a_texCoord0;				\n" +
				"}											\n";

			String fragmentShader =
				"#ifdef GL_ES											\n" +
				"precision mediump float;								\n" +
				"#endif													\n" +
				"uniform sampler2D u_texture;							\n" +
				"varying vec2 v_TexCoord;								\n" +
				"void main()											\n" +
				"{														\n" +
				"	gl_FragColor = texture2D( u_texture, v_TexCoord );	\n" +
				"}														\n";

		OrthographicAlignedMesh.shaderProgram = new ShaderProgram( vertexShader, fragmentShader );

		if( OrthographicAlignedMesh.shaderProgram.isCompiled() == false )
			throw new IllegalStateException( OrthographicAlignedMesh.shaderProgram.getLog() );
	}

	public static OrthographicAlignedMesh create( String mesh, Texture texture, Vector2 tilePosition )
	{
		OrthographicAlignedMesh m = new OrthographicAlignedMesh();

		try
		{
			InputStream in = Gdx.files.internal( mesh ).read();
			m.model_workaround = G3dtLoader.loadStillModel( in, true );
			in.close();

			m.model = new UStillModel( m.model_workaround.subMeshes );

			m.texture = texture;
			m.textureAttribute = new TextureAttribute(m.texture, 0, "textureAttributes");
			m.material = new Material("default", m.textureAttribute);
			m.model.setMaterial( m.material );

			if(tilePosition != null)
			{
				m.setTilePosition( (int)tilePosition.x, (int)tilePosition.y );
			}
			else
			{
				m.setPosition( 0, 0 );
			}

			//
			// apply horizontal fov scaling distortion and blender factors
			//

			// Blender => cube 14.2x14.2 meters = one tile (256px) w/ far plane @48
			// (256px are 14.2mt w/ 18px/mt)
			// I'm lazy and want Blender to work with 10x10mt instead, so a 1.42f
			// factor for this scaling: also, since the far plane is suboptimal at
			// just 48, i want 5 times more space on the z-axis, so here's another
			// scaling factor creeping up.
			float blenderToUracer = 5f * 1.42f;
			m.setScalingFactor( Director.scalingStrategy.meshScaleFactor * blenderToUracer * Director.scalingStrategy.to256 );
			m.setRotation( 0, 0, 0, 0 );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return m;
	}

	public static OrthographicAlignedMesh create( String mesh, Texture texture )
	{
		return OrthographicAlignedMesh.create( mesh, texture, null );
	}

	public TextureAttribute getTextureAttribute()
	{
		return textureAttribute;
	}

	public void setPositionOffsetPixels( int offsetPxX, int offsetPxY )
	{
		positionOffsetPx.x = offsetPxX;
		positionOffsetPx.y = offsetPxY;
	}

	/*
	 * @param x_index the x-axis index of the tile
	 * @param x_index the y-axis index of the tile
	 *
	 * @remarks The origin (0,0) is at the top-left corner
	 */
	public void setTilePosition( int tileIndexX, int tileIndexY )
	{
		positionPx.set( Convert.tileToPx( tileIndexX, tileIndexY ) );
	}

	/**
	 * Sets the world position in pixels, top-left origin.
	 * @param posPxX
	 * @param posPxY
	 */
	public void setPosition( float posPxX, float posPxY )
	{
		positionPx.set( Director.positionFor( posPxX, posPxY ) );
	}

	/**
	 * Sets the world position in pixels, top-left origin.
	 * @param x
	 * @param y
	 */
	public void setPositionUnscaled( float x, float y )
	{
		positionPx.set( x, y );
	}

	public float iRotationAngle;
	public Vector3 iRotationAxis = new Vector3();

	public void setRotation( float angle, float x_axis, float y_axis, float z_axis )
	{
		iRotationAngle = angle;
		iRotationAxis.set( x_axis, y_axis, z_axis );
	}

	public void setScalingFactor( float factor )
	{
		scalingFactor = factor;
		scaleAxis.set( scale, scale, scale );
	}

	public void setScale( float scale )
	{
		this.scale = scalingFactor * scale;
		scaleAxis.set( this.scale, this.scale, this.scale );
	}

	public void render( GL20 gl, OrthographicCamera orthoCamera, PerspectiveCamera perspCamera )
	{
		ShaderProgram shader = OrthographicAlignedMesh.shaderProgram;
//		texture.bind(0);

//		material.bind();
		shader.begin();
//		shader.setUniformf( "u_texture", 0 );

		tmp_vec.x = Convert.scaledPixels( positionOffsetPx.x - orthoCamera.position.x ) + orthoCamera.viewportWidth / 2 + positionPx.x;
		tmp_vec.y = Convert.scaledPixels( positionOffsetPx.y + orthoCamera.position.y ) + orthoCamera.viewportHeight / 2 - positionPx.y;
		tmp_vec.z = 1;

		perspCamera.unproject( tmp_vec );

		mtx_model.idt();
		mtx_model.setToTranslation( tmp_vec.x, tmp_vec.y, -(perspCamera.far - perspCamera.position.z) );

		Matrix4.mul( mtx_model.val, tmp_mtx.setToRotation( iRotationAxis, iRotationAngle ).val );
		Matrix4.mul( mtx_model.val, tmp_mtx.setToScaling( scaleAxis ).val );

		// proj * view
		mtx_combined.set( perspCamera.combined );

		// comb = comb * model (fast mul)
		Matrix4.mul( mtx_combined.val, mtx_model.val );

		shader.setUniformMatrix( "u_mvpMatrix", mtx_combined );
		model.render( shader );

		shader.end();
	}

	public void dispose()
	{
//		mesh.dispose();
		model.dispose();
		model_workaround.dispose();
//		texture.dispose();
	}
}
