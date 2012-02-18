package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.Convert;

public class LevelRenderer
{
	private PerspectiveCamera camPersp;
	private OrthographicCamera camOrtho;
	private ShaderProgram treeShader;

	public LevelRenderer(PerspectiveCamera persp, OrthographicCamera ortho)
	{
		camPersp = persp;
		camOrtho = ortho;

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
			"	vec4 texel = texture2D( u_texture, v_TexCoord );	\n" +
			"	if(texel.a < 0.5) discard;	\n" +
			"	gl_FragColor = texel;	\n" +
			"}														\n";

		ShaderProgram.pedantic = false;
		treeShader = new ShaderProgram( vertexShader, fragmentShader );

		if( treeShader.isCompiled() == false )
			throw new IllegalStateException( treeShader.getLog() );
	}

	public void renderWalls(GL20 gl, TrackWalls walls)
	{
		if(walls.walls.size() > 0)
		{
			gl.glDisable( GL20.GL_CULL_FACE );
			gl.glEnable( GL20.GL_BLEND );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );
			renderOrthographicAlignedModels( gl, walls.walls );
		}
	}

	public void renderTrees(GL20 gl, TrackTrees trees)
	{
		if(trees.trees.size() > 0)
		{
			trees.transform( camPersp, camOrtho );

			gl.glDisable( GL20.GL_BLEND );
			gl.glEnable( GL20.GL_CULL_FACE );

			Art.meshTreeTrunk.bind();

			treeShader.begin();

			// trunk
			for( int i = 0; i < trees.trees.size(); i++ )
			{
				TreeStillModel m = trees.trees.get( i );
				treeShader.setUniformMatrix( "u_mvpMatrix", m.transformed );
				m.trunk.render(treeShader, GL20.GL_TRIANGLES );
			}


			// transparent foliage
			gl.glEnable( GL20.GL_BLEND );
			gl.glDisable( GL20.GL_CULL_FACE );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );

			for( int i = 0; i < trees.trees.size(); i++ )
			{
				TreeStillModel m = trees.trees.get( i );
				if( i == 0 )
				{
					m.material.bind(treeShader);
				} else
				if( !trees.trees.get(i - 1).material.equals(m.material) )
				{
					m.material.bind(treeShader);
//					System.out.println("switched");
				}

				treeShader.setUniformMatrix( "u_mvpMatrix", m.transformed );
				m.leaves.render(treeShader, GL20.GL_TRIANGLES );
			}
			gl.glDisable( GL20.GL_BLEND );

			treeShader.end();
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix4 mtx2 = new Matrix4();
	public void renderOrthographicAlignedModels(GL20 gl, ArrayList<OrthographicAlignedStillModel> models)
	{
		OrthographicAlignedStillModel m;

		float meshZ = -(camPersp.far - camPersp.position.z);

		ShaderProgram shader = OrthographicAlignedStillModel.shaderProgram;
		shader.begin();

		for( int i = 0; i < models.size(); i++ )
		{
			m = models.get( i );

			// compute position
			tmpvec.x = Convert.scaledPixels( m.positionOffsetPx.x - camOrtho.position.x ) + Director.halfViewport.x + m.positionPx.x;
			tmpvec.y = Convert.scaledPixels( m.positionOffsetPx.y + camOrtho.position.y ) + Director.halfViewport.y - m.positionPx.y;
			tmpvec.z = 1;

			// transform to world space
			camPersp.unproject( tmpvec );

			// build model matrix
			// TODO: support proper rotation now that Mat3/Mat4 supports opengl-style rotation/translation/scaling
			mtx.setToTranslation( tmpvec.x, tmpvec.y, meshZ );
			Matrix4.mul( mtx.val, mtx2.setToRotation( m.iRotationAxis, m.iRotationAngle ).val );
			Matrix4.mul( mtx.val, mtx2.setToScaling( m.scaleAxis ).val );

			// comb = (proj * view) * model (fast mul)
			Matrix4.mul( mtx2.set( camPersp.combined ).val, mtx.val );

			shader.setUniformMatrix( "u_mvpMatrix", mtx2 );

			// avoid rebinding same textures
			if( i == 0 )
			{
				m.material.bind(shader);
			} else
			if( !models.get(i - 1).material.equals(m.material) )
			{
				m.material.bind(shader);
			}

			m.render(gl);
		}

		shader.end();
	}

}
