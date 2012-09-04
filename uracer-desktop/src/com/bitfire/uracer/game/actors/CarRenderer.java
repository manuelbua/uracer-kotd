
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;

public final class CarRenderer {
	private Sprite facet;
	private Sprite ambientOcclusion;
	private TextureRegion region;
	private float alpha;
	private ShaderProgram shader;

	// locally cached values
	private float occlusionOffX, occlusionOffY;
	private float facetOffX, facetOffY;

	public CarRenderer (CarModel model, CarPreset.Type type) {
		facet = new Sprite();
		ambientOcclusion = new Sprite();
		shader = null;
		setAspect(model, type);
	}

	public void setAspect (CarModel model, CarPreset.Type type) {
		// car
		this.region = Art.cars.findRegion(type.regionName);
		facet.setRegion(region);
		facet.setSize(Convert.mt2px(model.width), Convert.mt2px(model.length));
		facet.setOrigin(facet.getWidth() / 2, facet.getHeight() / 2);

		facetOffX = facet.getOriginX();
		facetOffY = facet.getOriginY();

		// ambient occlusion
		ambientOcclusion.setRegion(Art.carAmbientOcclusion);
		ambientOcclusion.setSize(facet.getWidth(), facet.getHeight());
		ambientOcclusion.setScale(2f, 2.3f);
		ambientOcclusion.setOrigin(ambientOcclusion.getWidth() / 2, ambientOcclusion.getHeight() / 2);

		occlusionOffX = ambientOcclusion.getOriginX();
		occlusionOffY = ambientOcclusion.getOriginY();
	}

	public void setShader (ShaderProgram program) {
		shader = program;
	}

	public Sprite getFacet () {
		return facet;
	}

	public TextureRegion getTextureRegion () {
		return region;
	}

	public void setAlpha (float alpha) {
		this.alpha = alpha;
	}

	public float getAlpha () {
		return alpha;
	}

// public void renderDepth( Matrix4 projTrans, EntityRenderState renderState ) {
// ShaderProgram depthgen = Art.depthMapGen;
//
// depthgen.begin();
// depthgen.setUniformMatrix( "u_projTrans", projTrans );
//
// facet.setPosition( renderState.position.x - facet.getOriginX(), renderState.position.y - facet.getOriginY() );
// facet.setRotation( renderState.orientation );
//
// depthgen.end();
// }

	public void renderShadows (SpriteBatch batch, EntityRenderState state) {
		ambientOcclusion.setPosition(state.position.x - occlusionOffX, state.position.y - occlusionOffY);
		ambientOcclusion.setRotation(state.orientation);
		ambientOcclusion.draw(batch, 0.65f * alpha);
	}

	public void render (SpriteBatch batch, EntityRenderState state) {
		facet.setPosition(state.position.x - facetOffX, state.position.y - facetOffY);
		facet.setRotation(state.orientation);

		if (shader != null) {
			batch.setShader(shader);
		}

		facet.draw(batch, alpha);

		if (shader != null) {
			batch.setShader(null);
		}
	}
}
