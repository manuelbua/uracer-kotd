package com.bitfire.uracer.factories;

import java.io.IOException;
import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
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
		Missing, Palm, House, Tribune, Tower, Arch, WallHorizontal, WallTopRightOuter, WallTopRightInner, LPTree
	}

	private static LongMap<StillModel> cachedModels;

	public static void init()
	{
		cachedModels = new LongMap<StillModel>();
	}

	public static OrthographicAlignedStillModel create( String meshType, float posPxX, float posPxY, float scale )
	{
		ModelMesh type = fromString( meshType );
		return ModelFactory.create( type, posPxX, posPxY, scale );
	}

	private static StillModel getModel( String model )
	{
		StillModel m = null;
		long modelHash = Hash.RSHash(model);
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

		case House:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/house.g3dt"), Art.meshHouse );
			break;

		case Tower:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/tower.g3dt"), Art.meshTower );
			break;

		case Arch:
			stillModel = new OrthographicAlignedStillModel( getModel("data/3d/models/test_arch.g3dt"), Art.mesh_test_arch_rusty);
			break;

		case LPTree:
			stillModel = new LPTreeStillModel( getModel("data/3d/models/LPTree1.g3dt"), Art.meshLPTree1 );
			break;

		case WallHorizontal:
			stillModel = new OrthographicAlignedStillModel( getModel("data/track/h.g3dt"), Art.trackWall );
			stillModel.getTextureAttribute().uWrap = TextureWrap.Repeat.getGLEnum();
			stillModel.getTextureAttribute().vWrap = TextureWrap.Repeat.getGLEnum();
			break;

		case WallTopRightOuter:
			stillModel = new OrthographicAlignedStillModel( getModel("data/track/tr-outer.g3dt"), Art.trackWall );
			stillModel.getTextureAttribute().uWrap = TextureWrap.Repeat.getGLEnum();
			stillModel.getTextureAttribute().vWrap = TextureWrap.Repeat.getGLEnum();
			break;

		case WallTopRightInner:
			stillModel = new OrthographicAlignedStillModel( getModel("data/track/tr-inner.g3dt"), Art.trackWall );
			stillModel.getTextureAttribute().uWrap = TextureWrap.Repeat.getGLEnum();
			stillModel.getTextureAttribute().vWrap = TextureWrap.Repeat.getGLEnum();
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

	private static ModelMesh fromString(String mesh)
	{
		if(mesh.equalsIgnoreCase( "palm" )) return ModelMesh.Palm;
		if(mesh.equalsIgnoreCase( "house" )) return ModelMesh.House;
		if(mesh.equalsIgnoreCase( "tribune" )) return ModelMesh.Tribune;
		if(mesh.equalsIgnoreCase( "tower" )) return ModelMesh.Tower;
		if(mesh.equalsIgnoreCase( "lptree1" )) return ModelMesh.LPTree;

		if(mesh.equalsIgnoreCase( "test_arch" )) return ModelMesh.Arch;

		return ModelMesh.Missing;
	}
}
