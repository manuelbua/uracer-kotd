package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;

public class LPTreeStillModel extends OrthographicAlignedStillModel
{
	public LPTreeStillModel( StillModel aModel, Texture aTexture )
	{
		super(aModel, aTexture);
	}

	@Override
	public void render(GL20 gl)
	{
		// trunk first
		model.subMeshes[1].mesh.render(OrthographicAlignedStillModel.shaderProgram, model.subMeshes[1].primitiveType);

		// transparent foliage
		gl.glEnable( GL20.GL_BLEND );
		gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );

		model.subMeshes[0].mesh.render(OrthographicAlignedStillModel.shaderProgram, model.subMeshes[0].primitiveType);

		gl.glDisable( GL20.GL_BLEND );
	}
}
