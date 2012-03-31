package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.factories.Box2DFactory;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class TrackWalls {
	public final ArrayList<OrthographicAlignedStillModel> walls = new ArrayList<OrthographicAlignedStillModel>();

	public TrackWalls() {
	}

	public void dispose() {
		for( int i = 0; i < walls.size(); i++ ) {
			walls.get( i ).dispose();
		}

		walls.clear();
	}

	public void createWalls(World world) {
		if( MapUtils.hasObjectGroup( MapUtils.LayerWalls ) ) {
			Vector2 fromMt = new Vector2();
			Vector2 toMt = new Vector2();
			Vector2 offsetMt = new Vector2();

			// create material
			TextureAttribute ta = new TextureAttribute( Art.meshTrackWall, 0, "textureAttributes" );
			ta.uWrap = TextureWrap.Repeat.getGLEnum();
			ta.vWrap = TextureWrap.Repeat.getGLEnum();
			Material mat = new Material( "trackWall", ta );

			TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerWalls );
			for( int i = 0; i < group.objects.size(); i++ ) {
				TiledObject o = group.objects.get( i );

				ArrayList<Vector2> points = MapUtils.extractPolyData( o.polyline );
				if( points.size() >= 2 ) {
					float factor = GameData.scalingStrategy.invTileMapZoomFactor;
					float wallSizeMt = 0.3f * factor;
					float[] mags = new float[ points.size() - 1 ];

					offsetMt.set( o.x, o.y );
					offsetMt.set( Convert.px2mt( offsetMt ) );

					fromMt.set( Convert.px2mt( points.get( 0 ) ) ).add( offsetMt ).mul( factor );
					fromMt.y = Director.worldSizeScaledMt.y - fromMt.y;

					for( int j = 1; j <= points.size() - 1; j++ ) {
						toMt.set( Convert.px2mt( points.get( j ) ) ).add( offsetMt ).mul( factor );
						toMt.y = Director.worldSizeScaledMt.y - toMt.y;

						// create box2d wall
						Box2DFactory.createWall( world, fromMt, toMt, wallSizeMt, 0f );

						// compute magnitude
						mags[j - 1] = (float)Math.sqrt( (toMt.x - fromMt.x) * (toMt.x - fromMt.x) + (toMt.y - fromMt.y) * (toMt.y - fromMt.y) );

						fromMt.set( toMt );
					}

					Mesh mesh = buildMesh( points, mags );

					StillSubMesh[] subMeshes = new StillSubMesh[ 1 ];
					subMeshes[0] = new StillSubMesh( "wall", mesh, GL10.GL_TRIANGLES );

					OrthographicAlignedStillModel model = new OrthographicAlignedStillModel( new StillModel( subMeshes ), mat );

					model.setPosition( o.x, o.y );
					model.setScale( 1 );

					walls.add( model );
				}
			}
		}
	}

	private Mesh buildMesh( ArrayList<Vector2> points, float[] magnitudes ) {
		Vector2 in = new Vector2();
		MathUtils.random.setSeed( Long.MIN_VALUE );

		// scaling factors
		float factor = GameData.scalingStrategy.invTileMapZoomFactor;
		float oneOnWorld3DFactor = 1f / OrthographicAlignedStillModel.World3DScalingFactor;
		float wallHeightMt = 5f * factor * oneOnWorld3DFactor;
		float textureScalingU = 1f;
		float coordU = 1f;
		float coordV = 3f;

		// jitter
		float jitterPositional = .5f * factor * oneOnWorld3DFactor;
		// float jitterAltitudinal = 3f * factor * oneOnWorld3DFactor;
		boolean addJitter = true;

		int vertexCount = points.size() * 2;
		int indexCount = (points.size() - 1) * 6;

		int vertSize = 5;	// x, y, z, u, v
		float[] verts = new float[ vertSize * vertexCount ];
		short[] indices = new short[ indexCount ];
		float mag, prevmag;
		mag = prevmag = magnitudes[0];

		// add input (interleaved w/ later filled dupes w/ just a meaningful z-coordinate)
		for( int i = 0, j = 0, vc = 0, vci = 0; i < points.size(); i++, j += 2 * vertSize ) {
			int magidx = i - 1;
			if( magidx < 0 ) magidx = 0;
			mag = AMath.lerp( prevmag, magnitudes[magidx], .25f );
			prevmag = mag;

			coordU = mag * textureScalingU;

			in.set( Convert.px2mt( points.get( i ) ) ).mul( factor * oneOnWorld3DFactor );

			// base
			verts[j + X1] = in.x;
			verts[j + Y1] = -in.y;
			verts[j + Z1] = 0;

			// elevation
			verts[j + X2] = in.x + (addJitter ? MathUtils.random( -jitterPositional, jitterPositional ) : 0);
			verts[j + Y2] = -in.y + (addJitter ? MathUtils.random( -jitterPositional, jitterPositional ) : 0);
			verts[j + Z2] = wallHeightMt;// + (addJitter? MathUtils.random( -jitterAltitudinal, jitterAltitudinal ) :
											// 0);

			// tex coords
			verts[j + U1] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V1] = coordV;

			verts[j + U2] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V2] = 0f;

			vc += 2;

			if( vc > 2 ) {
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 4);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 1);
			}
		}

		Mesh mesh = new Mesh( true, vertexCount, indexCount, new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ), new VertexAttribute(
				Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0" ) );

		mesh.setVertices( verts );
		mesh.setIndices( indices );

		return mesh;
	}

	static public final int X1 = 0;
	static public final int Y1 = 1;
	static public final int Z1 = 2;
	static public final int U1 = 3;
	static public final int V1 = 4;
	static public final int X2 = 5;
	static public final int Y2 = 6;
	static public final int Z2 = 7;
	static public final int U2 = 8;
	static public final int V2 = 9;
}
