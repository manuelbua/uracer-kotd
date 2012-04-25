package com.bitfire.uracer.game.world.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.utils.ShaderLoader;

/** The model is expected to follow the z-up convention.
 *
 * @author manuel */
public class OrthographicAlignedStillModel {
	public UStillModel model;
	public Material material;
	public BoundingBox localBoundingBox = new BoundingBox();
	public BoundingBox boundingBox = new BoundingBox();

	public static ShaderProgram shader = null;

	// Blender => cube 14.2x14.2 meters = one tile (256px) w/ far plane @48
	// (256px are 14.2mt w/ 18px/mt)
	// I'm lazy and want Blender to work with 10x10mt instead, so a 1.42f
	// factor for this scaling: also, since the far plane is suboptimal at
	// just 48, i want 5 times more space on the z-axis, so here's another
	// scaling factor creeping up.
	public static float World3DScalingFactor = 1.42222f;
	protected static float BlenderToURacer = 5f * World3DScalingFactor;

	// scale
	private float scale, scalingFactor;
	public Vector3 scaleAxis = new Vector3();

	// position
	public Vector2 positionOffsetPx = new Vector2( 0, 0 );
	public Vector2 positionPx = new Vector2();

	// explicitle initialize the static iShader member
	// (Android: statics need to be re-initialized!)
	private void loadShaders() {
		// @formatter:off
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
		// @formatter:on

		if( !(shader instanceof ShaderProgram) ) {
			ShaderProgram.pedantic = false;
			shader = ShaderLoader.fromString( vertexShader, fragmentShader, "OASM::vert", "OASM::frag" );
		}
	}

	public OrthographicAlignedStillModel( StillModel aModel, Material material, ScalingStrategy strategy ) {
		loadShaders();

		try {
			model = new UStillModel( aModel.subMeshes.clone() );

			this.material = material;
			model.setMaterial( this.material );

			model.getBoundingBox( localBoundingBox );
			boundingBox.set( localBoundingBox );

			setScalingFactor( strategy.meshScaleFactor * BlenderToURacer * strategy.to256 );
			setPosition( 0, 0 );
			setRotation( 0, 0, 0, 0 );
		} catch( Exception e ) {
			Gdx.app.log( "OrthographicAlignedStillModel", e.getMessage() );
		}
	}

	public void dispose() {
		try {
			model.dispose();
		} catch( IllegalArgumentException e ) {
			// buffer already disposed
		}
	}

	public final void setPositionOffsetPixels( int offsetPxX, int offsetPxY ) {
		positionOffsetPx.x = offsetPxX;
		positionOffsetPx.y = offsetPxY;
	}

	/** Sets the world position in pixels, top-left origin.
	 *
	 * @param posPxX
	 * @param posPxY */
	public final void setPosition( float posPxX, float posPxY ) {
		// positionPx.set( GameData.Environment.gameWorld.positionFor( posPxX, posPxY ) );
		positionPx.set( posPxX, posPxY );
	}

	public float iRotationAngle;
	public Vector3 iRotationAxis = new Vector3();

	public final void setRotation( float angle, float x_axis, float y_axis, float z_axis ) {
		iRotationAngle = angle;
		iRotationAxis.set( x_axis, y_axis, z_axis );
	}

	private void setScalingFactor( float factor ) {
		scalingFactor = factor;
		scaleAxis.set( scale, scale, scale );
	}

	public final void setScale( float scale ) {
		this.scale = scalingFactor * scale;
		scaleAxis.set( this.scale, this.scale, this.scale );
	}
}
