package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;

public class TreeStillModel extends OrthographicAlignedStillModel
{
	public Mesh leaves, trunk;
	public Matrix4 transformed = new Matrix4();
	public BoundingBox boundingBox = new BoundingBox();
	public BoundingBox localBoundingBox = new BoundingBox();

	public TreeStillModel( StillModel aModel, Material material, String meshName )
	{
		super(aModel, material);

		trunk = model.getSubMesh( meshName + "trunk" ).mesh;
		leaves = model.getSubMesh( meshName + "leaves" ).mesh;
		model.getBoundingBox( localBoundingBox );
		boundingBox.set( localBoundingBox );
	}
}
