package com.bitfire.uracer.factories;

import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.bitfire.uracer.tiled.OrthographicAlignedMesh;

public class MeshFactory
{
	public static final int MeshPalm = 1;
	public static final int MeshHouse = 2;
	public static final int MeshTribune = 3;
	public static final int MeshTower = 4;
	public static final int MeshArch = 5;

	public static final int WallHorizontal = 10;
	public static final int WallTopRight = 12;


	public static OrthographicAlignedMesh create( String meshType, float posPxX, float posPxY, float scale )
	{
		int type = fromString( meshType );
		return MeshFactory.create( type, posPxX, posPxY, scale );
	}

	public static OrthographicAlignedMesh create( int meshType, float posPxX, float posPxY, float scale )
	{
		OrthographicAlignedMesh mesh = null;

		switch( meshType )
		{
		case MeshPalm:
			mesh = OrthographicAlignedMesh.create( "data/3d/palm.g3dt", "data/3d/palm.png" );
			break;

		case MeshTribune:
			mesh = OrthographicAlignedMesh.create( "data/3d/tribune.g3dt", "data/3d/tribune.png" );
			break;

		case MeshHouse:
			mesh = OrthographicAlignedMesh.create( "data/3d/house.g3dt", "data/3d/house.png" );
			break;

		case MeshTower:
			mesh = OrthographicAlignedMesh.create( "data/3d/tower.g3dt", "data/3d/tower.png" );
			break;

		case MeshArch:
//			mesh = null;
			mesh = OrthographicAlignedMesh.create( "data/3d/test_arch.g3dt", "data/3d/test_arch_rusty.jpg" );
			break;

		case WallHorizontal:
			mesh = OrthographicAlignedMesh.create( "data/3d/track/h.g3dt", "data/3d/track/wall.jpg" );
			mesh.getTextureAttribute().uWrap = TextureWrap.Repeat.getGLEnum();
			mesh.getTextureAttribute().vWrap = TextureWrap.Repeat.getGLEnum();
			break;

		case WallTopRight:
			mesh = OrthographicAlignedMesh.create( "data/3d/track/tr.g3dt", "data/3d/track/wall.jpg" );
			mesh.getTextureAttribute().uWrap = TextureWrap.Repeat.getGLEnum();
			mesh.getTextureAttribute().vWrap = TextureWrap.Repeat.getGLEnum();
			break;

		// missing mesh mesh
		default:
			mesh = OrthographicAlignedMesh.create( "data/3d/missing-mesh.g3dt", "data/3d/missing-mesh.png" );
		}

		if( mesh != null )
		{
			mesh.setPosition( posPxX, posPxY );
			mesh.setScale( scale );
		}

		return mesh;
	}

	private static int fromString(String mesh)
	{
		if(mesh.equalsIgnoreCase( "palm" )) return MeshPalm;
		if(mesh.equalsIgnoreCase( "house" )) return MeshHouse;
		if(mesh.equalsIgnoreCase( "tribune" )) return MeshTribune;
		if(mesh.equalsIgnoreCase( "tower" )) return MeshTower;

		if(mesh.equalsIgnoreCase( "test_arch" )) return MeshArch;

		return 0;
	}

}
