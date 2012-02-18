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
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class TrackTrees
{
	public final ArrayList<TreeStillModel> trees = new ArrayList<TreeStillModel>();

	public void dispose()
	{
		for(int i = 0; i < trees.size(); i++)
		{
			trees.get( i ).dispose();
		}

		trees.clear();
	}

	private int nextIndexFor(TreeStillModel model)
	{
		for(int i = 0; i < trees.size(); i++)
		{
			if(model.material.equals( trees.get(i).material ))
				return i;
		}

		return 0;
	}

	public void createTrees()
	{
		if( MapUtils.hasObjectGroup( MapUtils.LayerTrees ) )
		{
			MathUtils.random.setSeed( Long.MAX_VALUE );
			TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerTrees );
			for( int i = 0; i < group.objects.size(); i++ )
			{
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( MapUtils.MeshScale ) != null )
					scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );

				TreeStillModel model = ModelFactory.createTree( o.type, o.x, o.y, scale );
				if( model != null )
				{
					model.setRotation( MathUtils.random( 0f, 90f ), 0, 0, 1f );
					trees.add( nextIndexFor( model ), model );
				}
			}
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 tmpmtx = new Matrix4();
	private Matrix4 tmpmtx2 = new Matrix4();
	public void transform(PerspectiveCamera camPersp, OrthographicCamera camOrtho)
	{
		float meshZ = -(camPersp.far - camPersp.position.z);

		for( int i = 0; i < trees.size(); i++ )
		{
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
			Matrix4.mul( tmpmtx.val, tmpmtx2.setToRotation(m.iRotationAxis, m.iRotationAngle).val );
			Matrix4.mul( tmpmtx.val, tmpmtx2.setToScaling(m.scaleAxis).val );

			// comb = (proj * view) * model (fast mul)
			Matrix4.mul( transf.set(camPersp.combined).val, tmpmtx.val );
		}
	}
}
