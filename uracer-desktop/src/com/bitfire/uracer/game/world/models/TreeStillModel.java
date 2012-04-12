package com.bitfire.uracer.game.world.models;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.ScalingStrategy;

public class TreeStillModel extends OrthographicAlignedStillModel {
	public Mesh leaves, trunk;
	public StillSubMesh smLeaves, smTrunk;
	public Matrix4 transformed = new Matrix4();
	public BoundingBox boundingBox = new BoundingBox();
	public BoundingBox localBoundingBox = new BoundingBox();

	public TreeStillModel( StillModel aModel, Material material, String meshName, ScalingStrategy strategy ) {
		super( aModel, material, strategy );

		smTrunk = model.getSubMesh( meshName + "trunk" );
		smLeaves = model.getSubMesh( meshName + "leaves" );
		trunk = smTrunk.mesh;
		leaves = smLeaves.mesh;

		model.getBoundingBox( localBoundingBox );
		boundingBox.set( localBoundingBox );
	}
}
