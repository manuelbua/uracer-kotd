
package com.bitfire.uracer.game.world.models;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.u3d.materials.Material;
import com.bitfire.uracer.u3d.still.StillModel;
import com.bitfire.uracer.u3d.still.StillSubMesh;

public class TreeStillModel extends OrthographicAlignedStillModel {
	public Mesh leaves, trunk;
	public StillSubMesh smLeaves, smTrunk;
	public Matrix4 transformed = new Matrix4();
	public Matrix4 mtxmodel = new Matrix4();
	public BoundingBox boundingBox = new BoundingBox();
	public BoundingBox localBoundingBox = new BoundingBox();

	public TreeStillModel (StillModel aModel, Material material, String meshName) {
		super(aModel, material);

		smTrunk = model.getSubMesh(meshName + "trunk");
		smLeaves = model.getSubMesh(meshName + "leaves");
		if (smTrunk != null) {
			trunk = smTrunk.mesh;
		}

		if (smLeaves != null) {
			leaves = smLeaves.mesh;
		}

		model.getBoundingBox(localBoundingBox);
		boundingBox.set(localBoundingBox);
	}
}
