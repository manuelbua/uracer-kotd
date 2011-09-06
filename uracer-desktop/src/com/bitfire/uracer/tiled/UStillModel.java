package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class UStillModel extends StillModel
{

	public UStillModel( StillSubMesh[] subMeshes )
	{
		super( subMeshes );
	}

	@Override
	public void render()
	{
		super.render();
	}

	@Override
	public void render( ShaderProgram program )
	{
		int len = subMeshes.length;
		for (int i = 0; i < len; i++) {
			StillSubMesh subMesh = subMeshes[i];
			if (i == 0) {
				subMesh.material.bind(program);
			} else if (!subMeshes[i - 1].material.equals(subMesh.material)) {
				subMesh.material.bind(program);
			}
			subMesh.mesh.render(program, subMesh.primitiveType);
		}
	}
}
