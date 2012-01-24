package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
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
public class OrthographicAlignedStillModel
{
	private UStillModel model;
	private Material material;
	private Texture texture;
	private TextureAttribute textureAttribute;

	// matrix state
	private Matrix4 mtx_model = new Matrix4();
	private Matrix4 mtx_combined = new Matrix4();

	protected static ShaderProgram shaderProgram = null;
	protected ShaderProgram customShader = null;

	// scale
	private float scale, scalingFactor;
	private Vector3 scaleAxis = new Vector3();

	// position
	private Vector2 positionOffsetPx = new Vector2( 0, 0 );
	private Vector2 positionPx = new Vector2();

	private static float meshZ;
	private static OrthographicCamera camOrtho;
	private static PerspectiveCamera camPersp;

	// temporaries
	private Vector3 tmp_vec = new Vector3();
	private Matrix4 tmp_mtx = new Matrix4();


	// explicitle initialize the static iShader member
	// (Android: statics need to be re-initialized!)
	public static void initialize( OrthographicCamera orthoCamera, PerspectiveCamera perspCamera )
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

		OrthographicAlignedStillModel.shaderProgram = new ShaderProgram( vertexShader, fragmentShader );

		if( OrthographicAlignedStillModel.shaderProgram.isCompiled() == false )
			throw new IllegalStateException( OrthographicAlignedStillModel.shaderProgram.getLog() );

		OrthographicAlignedStillModel.meshZ = -(perspCamera.far - perspCamera.position.z);
		camOrtho = orthoCamera;
		camPersp = perspCamera;
	}

	public static OrthographicAlignedStillModel create( StillModel model, Texture texture )
	{
		OrthographicAlignedStillModel m = new OrthographicAlignedStillModel();

		try
		{
			m.model = new UStillModel( model.subMeshes.clone() );

			// set material
			m.texture = texture;
			m.textureAttribute = new TextureAttribute(m.texture, 0, "textureAttributes");
			m.material = new Material("default", m.textureAttribute);
			m.model.setMaterial( m.material );

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

			m.setPosition( 0, 0 );
			m.setRotation( 0, 0, 0, 0 );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return m;
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

	private static void screenToWorld( Vector3 result, OrthographicCamera orthoCamera, PerspectiveCamera perspCamera, Vector2 positionOffsetPx, Vector2 positionPx )
	{
		result.x = Convert.scaledPixels( positionOffsetPx.x - orthoCamera.position.x ) + orthoCamera.viewportWidth / 2 + positionPx.x;
		result.y = Convert.scaledPixels( positionOffsetPx.y + orthoCamera.position.y ) + orthoCamera.viewportHeight / 2 - positionPx.y;
		result.z = 1;

		// transform to world space
		perspCamera.unproject( result );
	}

	public void setShader(ShaderProgram program)
	{
		customShader = program;
	}

	public void render( GL20 gl )
	{
		ShaderProgram shader = (customShader!=null) ? customShader : OrthographicAlignedStillModel.shaderProgram;
		shader.begin();

		// compute final position
		screenToWorld(tmp_vec, camOrtho, camPersp, positionOffsetPx, positionPx );

		mtx_model.idt();
		mtx_model.setToTranslation( tmp_vec.x, tmp_vec.y, OrthographicAlignedStillModel.meshZ );

		// TODO (when updating the local libgdx repo)
		// support proper rotation now that Mat3/Mat4 supports opengl-style rotation/translation/scaling
		Matrix4.mul( mtx_model.val, tmp_mtx.setToRotation( iRotationAxis, iRotationAngle ).val );
		Matrix4.mul( mtx_model.val, tmp_mtx.setToScaling( scaleAxis ).val );

		// proj * view
		mtx_combined.set( camPersp.combined );

		// comb = comb * model (fast mul)
		Matrix4.mul( mtx_combined.val, mtx_model.val );

		shader.setUniformMatrix( "u_mvpMatrix", mtx_combined );
		model.render( shader );

		shader.end();
	}

	public void dispose()
	{
		try {
			model.dispose();
		} catch( IllegalArgumentException e )
		{
			// buffer already disposed
		}
	}
}
