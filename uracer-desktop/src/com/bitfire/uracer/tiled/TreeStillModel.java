package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.math.Matrix4;

public class TreeStillModel extends OrthographicAlignedStillModel
{
	public Mesh leaves, trunk;
	public Matrix4 transformed = new Matrix4();

	public TreeStillModel( StillModel aModel, Material material, String meshName )
	{
		super(aModel, material);

		trunk = model.getSubMesh( meshName + "trunk" ).mesh;
		leaves = model.getSubMesh( meshName + "leaves" ).mesh;
	}

	@Override
	public void render(GL20 gl)
	{
	}
}
