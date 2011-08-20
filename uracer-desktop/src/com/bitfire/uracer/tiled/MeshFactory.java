package com.bitfire.uracer.tiled;

public class MeshFactory
{
	public static OrthographicAlignedMesh create( String meshType, float posPxX, float posPxY, float scale )
	{
		OrthographicAlignedMesh mesh = null;
		int type = MeshTypes.fromString( meshType );

		switch( type )
		{
		case MeshTypes.MeshPalm:
			mesh = OrthographicAlignedMesh.create( "data/3d/palm.g3dt", "data/3d/palm.png" );
			break;

		case MeshTypes.MeshTribune:
			mesh = OrthographicAlignedMesh.create( "data/3d/tribune.g3dt", "data/3d/tribune.png" );
			break;

		case MeshTypes.MeshHouse:
			mesh = OrthographicAlignedMesh.create( "data/3d/house.g3dt", "data/3d/house.png" );
			break;

		case MeshTypes.MeshTower:
			mesh = OrthographicAlignedMesh.create( "data/3d/tower.g3dt", "data/3d/tower.png" );
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
}
