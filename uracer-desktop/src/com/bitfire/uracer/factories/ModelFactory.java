package com.bitfire.uracer.factories;

import java.io.IOException;
import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.loaders.g3d.G3dtLoader;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.tiled.LPTreeStillModel;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.utils.Hash;

public class ModelFactory
{
	public enum ModelMesh {
		Missing, Palm, Tribune, LPTree
	}

	private static ModelMesh fromString(String mesh)
	{
		if(mesh.equalsIgnoreCase( "palm" )) return ModelMesh.Palm;
		if(mesh.equalsIgnoreCase( "tribune" )) return ModelMesh.Tribune;
		if(mesh.equalsIgnoreCase( "lptree1" )) return ModelMesh.LPTree;

		return ModelMesh.Missing;
	}

	public static OrthographicAlignedStillModel create( String meshType, float posPxX, float posPxY, float scale )
	{
		ModelMesh type = fromString( meshType );
		return ModelFactory.create( type, posPxX, posPxY, scale );
	}

	public static OrthographicAlignedStillModel create( ModelMesh modelMesh, float posPxX, float posPxY, float scale )
	{
		OrthographicAlignedStillModel stillModel = null;

		switch( modelMesh )
		{
		case Palm:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/palm.g3dt"), Art.meshPalm );
			break;

		case Tribune:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/tribune.g3dt"), Art.meshTribune );
			break;

		case LPTree:
			stillModel = new LPTreeStillModel( getModel("data/3d/models/LPTree1.g3dt"), Art.meshLPTree1 );
			break;

		// missing mesh mesh
		case Missing:
		default:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/missing-mesh.g3dt"), Art.meshMissing );
		}

		if( stillModel != null )
		{
			stillModel.setPosition( posPxX, posPxY );
			stillModel.setScale( scale );
		}

		return stillModel;
	}

	private static LongMap<StillModel> cachedModels = null;

	private static StillModel getModel( String model )
	{
		StillModel m = null;
		long modelHash = Hash.RSHash(model);

		if(cachedModels==null)
		{
			cachedModels = new LongMap<StillModel>();
		}

		if( cachedModels.containsKey( modelHash ))
		{
			return cachedModels.get( modelHash );
		}
		else
		{
			try
			{
				InputStream in = Gdx.files.internal( model ).read();
				m = G3dtLoader.loadStillModel( in, true );
				in.close();
				cachedModels.put( modelHash, m );
			} catch( IOException ioex )
			{
				ioex.printStackTrace();
			}
		}

		return m;
	}
}
