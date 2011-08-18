package com.bitfire.uracer.tiled;

import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g3d.loaders.g3d.G3dtLoader;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Director;

/**
 * The model is expected to follow the z-up convention.
 *
 * @author manuel
 *
 */
public class OrthographicAlignedMesh
{
	private TiledMap tileMap;

	private Mesh mesh;
	private StillModel model;
	private Texture texture;

	// matrix state
	private Matrix4 mtx_model = new Matrix4();
	private Matrix4 mtx_combined = new Matrix4();

	private static ShaderProgram shaderProgram = null;

	// scale
	private float scale, originalScale;
	private Vector3 scaleAxis = new Vector3();

	// position
	private Vector2 positionOffsetPx = new Vector2( 0, 0 );
	private Vector2 positionTile = new Vector2( 0, 0 );
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

	public static OrthographicAlignedMesh create( TiledMap map, String mesh, String texture, Vector2 tilePosition )
	{
		OrthographicAlignedMesh m = new OrthographicAlignedMesh();

		try
		{
			InputStream in = Gdx.files.internal( mesh ).read();
//			m.mesh = ObjLoader.loadObj( in, true );
			m.model = G3dtLoader.loadStillModel( in, true );
			Material material = new Material("default", new TextureAttribute(new Texture(Gdx.files.internal(texture)), 0, "tex0"));
			m.model.setMaterial( material );
			in.close();

			m.tileMap = map;
			m.texture = new Texture( Gdx.files.internal( texture ), Format.RGB565, false );
			m.positionTile = tilePosition;

			m.setScale( 1 );
			m.setRotation( 0, 0, 0, 0 );

			m.computeTilePosition();
		} catch( Exception e )
		{
			e.printStackTrace();
		}

		return m;
	}

	public static OrthographicAlignedMesh create( TiledMap map, String mesh, String texture )
	{
		return OrthographicAlignedMesh.create( map, mesh, texture, new Vector2( 0, 0 ) );
	}

	// compute ortographic-aligned tile position in pixels, w/ centroid at tile
	// center
	private void computeTilePosition()
	{
		positionPx.x = (positionTile.x * tileMap.tileWidth) + tileMap.tileWidth / 2;

		positionPx.y = ((positionTile.y - tileMap.height) * tileMap.tileHeight) + tileMap.tileHeight / 2;
	}

	public void setPositionOffsetPixels( int x, int y )
	{
		positionOffsetPx.x = x;
		positionOffsetPx.y = y;
	}

	/*
	 * @param x_index the x-axis index of the tile
	 *
	 * @param x_index the y-axis index of the tile
	 *
	 * @remarks The origin (0,0) is at the top-left corner
	 */
	public void setTilePosition( int x_index, int y_index )
	{
		positionTile.x = x_index;
		positionTile.y = y_index;
		computeTilePosition();
	}

	public void setPosition( float x, float y )
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

	public void setScale( float scale )
	{
		this.scale = originalScale = scale;
		scaleAxis.set( scale, scale, scale );
	}

	public void rescale( float factor )
	{
		scale = originalScale * factor;
		scaleAxis.set( scale, scale, scale );
	}

	public void render( GL20 gl, OrthographicCamera orthoCamera, PerspectiveCamera perspCamera )
	{
		ShaderProgram shader = OrthographicAlignedMesh.shaderProgram;

		gl.glActiveTexture( 0 );
		texture.bind();
		shader.begin();
		shader.setUniformf( "u_texture", 0 );

		// account for 3d origin being in the center of the screen
		// also apply orthocamera's zoom and user-specified offset
		float zoomFactor = Director.scalingStrategy.invTileMapZoomFactor;

		tmp_vec.set(
				positionPx.x * zoomFactor - (orthoCamera.position.x * zoomFactor - orthoCamera.viewportWidth / 2) + positionOffsetPx.x * zoomFactor,
				positionPx.y * zoomFactor + (orthoCamera.position.y * zoomFactor + orthoCamera.viewportHeight / 2) + positionOffsetPx.y * zoomFactor,
				1 );

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
//		mesh.render( shader, GL20.GL_TRIANGLES );
		model.render( shader );

		shader.end();
	}

	public void dispose()
	{
//		mesh.dispose();
		model.dispose();
		texture.dispose();
	}
}
