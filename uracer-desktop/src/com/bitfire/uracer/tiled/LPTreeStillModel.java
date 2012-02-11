package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.bitfire.uracer.Director;

public class LPTreeStillModel extends OrthographicAlignedStillModel
{
	public LPTreeStillModel( StillModel model, Texture texture )
	{
		this.model = new UStillModel( model.subMeshes.clone() );
		this.texture = texture;
		textureAttribute = new TextureAttribute(texture, 0, "textureAttributes");
		material = new Material("default", textureAttribute);
		model.setMaterial( material );

		setScalingFactor( Director.scalingStrategy.meshScaleFactor * BlenderToURacer * Director.scalingStrategy.to256 );

		setPosition( 0, 0 );
		setRotation( 0, 0, 0, 0 );
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
