package com.bitfire.uracer.factories;

import java.io.IOException;
import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.loaders.g3d.G3dtLoader;
import com.badlogic.gdx.graphics.g3d.loaders.wavefront.ObjLoader;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.tiled.TreeStillModel;
import com.bitfire.uracer.utils.Hash;

public class ModelFactory {
	public enum ModelMesh {
		Missing, Palm, Tribune, Tree_1, Tree_2, Tree_3, Tree_4, Tree_5, Tree_6, Tree_7, Tree_8, Tree_9
	}

	private static ModelMesh fromString( String mesh ) {
		if( mesh == null ) {
			return ModelMesh.Missing;
		} else if( mesh.equalsIgnoreCase( "palm" ) ) {
			return ModelMesh.Palm;
		} else if( mesh.equalsIgnoreCase( "tribune" ) ) {
			return ModelMesh.Tribune;
		} else if( mesh.equalsIgnoreCase( "tree-1" ) ) {
			return ModelMesh.Tree_1;
		} else if( mesh.equalsIgnoreCase( "tree-2" ) ) {
			return ModelMesh.Tree_2;
		} else if( mesh.equalsIgnoreCase( "tree-3" ) ) {
			return ModelMesh.Tree_3;
		} else if( mesh.equalsIgnoreCase( "tree-4" ) ) {
			return ModelMesh.Tree_4;
		} else if( mesh.equalsIgnoreCase( "tree-5" ) ) {
			return ModelMesh.Tree_5;
		} else if( mesh.equalsIgnoreCase( "tree-6" ) ) {
			return ModelMesh.Tree_6;
		} else if( mesh.equalsIgnoreCase( "tree-7" ) ) {
			return ModelMesh.Tree_7;
		} else if( mesh.equalsIgnoreCase( "tree-8" ) ) {
			return ModelMesh.Tree_8;
		} else if( mesh.equalsIgnoreCase( "tree-9" ) ) {
			return ModelMesh.Tree_9;
		}

		return ModelMesh.Missing;
	}

	public static OrthographicAlignedStillModel create( String meshType, float posPxX, float posPxY, float scale ) {
		ModelMesh type = fromString( meshType );
		return ModelFactory.create( type, posPxX, posPxY, scale );
	}

	public static OrthographicAlignedStillModel create( ModelMesh modelMesh, float posPxX, float posPxY, float scale ) {
		OrthographicAlignedStillModel stillModel = null;

		switch( modelMesh ) {
		case Palm:
			stillModel = new OrthographicAlignedStillModel( getModel( "data/3d/models/palm.g3dt" ), getMaterial( modelMesh, Art.meshPalm ) );
			break;

		case Tribune:
			stillModel = new OrthographicAlignedStillModel( getModel( "data/3d/models/tribune.g3dt" ), getMaterial( modelMesh, Art.meshTribune ) );
			break;

		// missing mesh mesh
		case Missing:
		default:
			stillModel = new OrthographicAlignedStillModel( getModel( "data/3d/models/missing-mesh.g3dt" ), getMaterial( modelMesh, Art.meshMissing ) );
		}

		if( stillModel != null ) {
			stillModel.setPosition( posPxX, posPxY );
			if( modelMesh != ModelMesh.Missing )
				stillModel.setScale( scale );
			else
				stillModel.setScale( 1 );
		}

		return stillModel;
	}

	public static TreeStillModel createTree( String meshType, float posPxX, float posPxY, float scale ) {
		ModelMesh type = fromString( meshType );
		return ModelFactory.createTree( type, posPxX, posPxY, scale );
	}

	public static TreeStillModel createTree( ModelMesh modelMesh, float posPxX, float posPxY, float scale ) {
		TreeStillModel stillModel = null;

		String treeModelName = "", treeMeshName = "";
		Texture leavesTexture = null;

		switch( modelMesh ) {
		case Tree_1:
			treeModelName = "tree-1.g3dt";
			treeMeshName = "tree_1_";
			leavesTexture = Art.meshTreeLeavesSpring[2];
			break;

		case Tree_2:
			treeModelName = "tree-2.g3dt";
			treeMeshName = "tree_2_";
			leavesTexture = Art.meshTreeLeavesSpring[0];
			break;

		case Tree_3:
			treeModelName = "tree-3.g3dt";
			treeMeshName = "tree_3_";
			leavesTexture = Art.meshTreeLeavesSpring[0];
			break;

		case Tree_4:
			treeModelName = "tree-4.g3dt";
			treeMeshName = "tree_4_";
			leavesTexture = Art.meshTreeLeavesSpring[1];
			break;

		case Tree_5:
			treeModelName = "tree-5.g3dt";
			treeMeshName = "tree_5_";
			leavesTexture = Art.meshTreeLeavesSpring[4];
			break;

		case Tree_6:
			treeModelName = "tree-6.g3dt";
			treeMeshName = "tree_6_";
			leavesTexture = Art.meshTreeLeavesSpring[5];
			break;

		case Tree_7:
			treeModelName = "tree-7.g3dt";
			treeMeshName = "tree_7_";
			leavesTexture = Art.meshTreeLeavesSpring[4];
			break;

		case Tree_8:
			treeModelName = "tree-8.g3dt";
			treeMeshName = "tree_8_";
			leavesTexture = Art.meshTreeLeavesSpring[3];
			break;

		case Tree_9:
			treeModelName = "tree-9.g3dt";
			treeMeshName = "tree_9_";
			leavesTexture = Art.meshTreeLeavesSpring[6];
			break;
		// missing mesh mesh
		// case Missing:
		// default:
		// stillModel = new OrthographicAlignedStillModel( getModel( "data/3d/models/missing-mesh.g3dt" ), getMaterial(
		// modelMesh, Art.meshMissing ) );

		}

		stillModel = new TreeStillModel( getModel( "data/3d/models/" + treeModelName ), getMaterial( modelMesh, leavesTexture ), treeMeshName );

		if( stillModel != null ) {
			stillModel.setPosition( posPxX, posPxY );
			if( modelMesh != ModelMesh.Missing )
				stillModel.setScale( scale );
			else
				stillModel.setScale( 1 );
		}

		return stillModel;
	}

	private static LongMap<Material> cachedMaterials = null;

	private static Material getMaterial( ModelMesh modelMesh, Texture texture ) {
		Material m = null;

		long materialHash = Hash.RSHash( modelMesh.toString() );
		if( cachedMaterials == null ) {
			cachedMaterials = new LongMap<Material>();
		}

		if( cachedMaterials.containsKey( materialHash ) ) {
			return cachedMaterials.get( materialHash );
		} else {
			TextureAttribute ta = new TextureAttribute( texture, 0, "textureAttributes" );
			m = new Material( "default", ta );

			cachedMaterials.put( materialHash, m );
		}

		return m;
	}

	private static LongMap<StillModel> cachedModels = null;

	private static StillModel getModel( String model ) {
		StillModel m = null;
		long modelHash = Hash.RSHash( model );

		if( cachedModels == null ) {
			cachedModels = new LongMap<StillModel>();
		}

		if( cachedModels.containsKey( modelHash ) ) {
			return cachedModels.get( modelHash );
		} else {
			try {
				String[] ext = model.split( "\\." );

				if( ext[1].equals( "g3dt" ) ) {
					// NO opengl coords, NO invert v
					InputStream in = Gdx.files.internal( model ).read();
					m = G3dtLoader.loadStillModel( in, true );
					in.close();
				} else if( ext[1].equals( "obj" ) ) {
					// y-forward, z-up
					ObjLoader l = new ObjLoader();
					m = l.loadObj( Gdx.files.internal( model ), true );
				}

				cachedModels.put( modelHash, m );
			} catch( IOException ioex ) {
				ioex.printStackTrace();
			}
		}

		return m;
	}
}
