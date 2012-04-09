package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class TrackTrees {
	public final ArrayList<TreeStillModel> trees = new ArrayList<TreeStillModel>();

	public TrackTrees() {
		// for( int i = 0; i < vtrans.length; i++)
		// vtrans[i] = new Vector3();

		// We want to differentiate tree meshes as much as we can
		// rotation will helps immensely, but non-orthogonal rotations
		// will cause the bounding box to get recomputed only approximately
		// thus loosing precision: orthogonal rotations instead provides high
		// quality AABB recomputation.
		//
		// We still have 4 variations for any given tree!
		rotations[0] = 0;
		rotations[1] = 90;
		rotations[2] = 180;
		rotations[3] = 270;
	}

	public void dispose() {
		for( int i = 0; i < trees.size(); i++ ) {
			trees.get( i ).dispose();
		}

		trees.clear();
	}

	private int nextIndexFor( TreeStillModel model ) {
		for( int i = 0; i < trees.size(); i++ ) {
			if( model.material.equals( trees.get( i ).material ) )
				return i;
		}

		return 0;
	}

	private float[] rotations = new float[ 4 ];

	public void createTrees() {
		if( MapUtils.hasObjectGroup( MapUtils.LayerTrees ) ) {
			MathUtils.random.setSeed( Long.MAX_VALUE );
			TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerTrees );
			for( int i = 0; i < group.objects.size(); i++ ) {
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( MapUtils.MeshScale ) != null )
					scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );

				TreeStillModel model = null;
				if( o.type != null ) {
					model = ModelFactory.createTree( o.type, o.x, o.y, scale );
				} else {
					System.out.println( "# load error, no type was given for the tree #" + (i + 1) );
				}

				if( model != null ) {
					// model.setRotation( MathUtils.random( -180f, 180f ), 0, 0, 1f );
					model.setRotation( rotations[MathUtils.random( 0, 3 )], 0, 0, 1f );
					trees.add( nextIndexFor( model ), model );
				}
			}
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 tmpmtx = new Matrix4();
	private Matrix4 tmpmtx2 = new Matrix4();

	// private Vector3[] vtrans = new Vector3[8];
	public void transform( PerspectiveCamera camPersp, OrthographicCamera camOrtho ) {
		float meshZ = -(camPersp.far - camPersp.position.z);

		for( int i = 0; i < trees.size(); i++ ) {
			TreeStillModel m = trees.get( i );

			Matrix4 transf = m.transformed;

			// compute position
			tmpvec.x = Convert.scaledPixels( m.positionOffsetPx.x - camOrtho.position.x ) + Director.halfViewport.x + m.positionPx.x;
			tmpvec.y = Convert.scaledPixels( m.positionOffsetPx.y + camOrtho.position.y ) + Director.halfViewport.y - m.positionPx.y;
			tmpvec.z = 1;

			// transform to world space
			camPersp.unproject( tmpvec );

			// build model matrix
			tmpmtx.setToTranslation( tmpvec.x, tmpvec.y, meshZ );
			Matrix4.mul( tmpmtx.val, tmpmtx2.setToScaling( m.scaleAxis ).val );
			Matrix4.mul( tmpmtx.val, tmpmtx2.setToRotation( m.iRotationAxis, m.iRotationAngle ).val );

			// comb = (proj * view) * model (fast mul)
			Matrix4.mul( transf.set( camPersp.combined ).val, tmpmtx.val );

			// transform the bounding box
			m.boundingBox.inf().set( m.localBoundingBox );
			m.boundingBox.mul( tmpmtx );

			// create an AABB out of the corners of the original
			// AABB transformed by the model matrix
			// bb.inf();
			// Vector3[] corners = m.localBoundingBox.getCorners();
			// for(int k = 0; k < corners.length; k++)
			// {
			// vtrans[k].x = corners[k].x;
			// vtrans[k].y = corners[k].y;
			// vtrans[k].z = corners[k].z;
			// vtrans[k].mul( tmpmtx );
			// bb.ext(vtrans[k]);
			// }
			//
			// m.boundingBox.inf();
			// m.boundingBox.set( bb );
		}
	}
}
