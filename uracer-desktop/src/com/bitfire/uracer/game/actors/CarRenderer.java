
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.ModelFactory.ModelMesh;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;

public final class CarRenderer {
	private Sprite facet;
	private Sprite ambientOcclusion;
	private TextureRegion region;
	private float alpha;

	// locally cached values
	private float occlusionOffX, occlusionOffY;
	private float facetOffX, facetOffY;
	private OrthographicAlignedStillModel carStillModel;

	public CarRenderer (CarModel model, CarPreset.Type type) {
		facet = new Sprite();
		ambientOcclusion = new Sprite();
		carStillModel = ModelFactory.create(ModelMesh.Car, 0, 0, 1);
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
		ambientOcclusion.setScale(1.9f, 1.52f);
		ambientOcclusion.setOrigin(ambientOcclusion.getWidth() / 2, ambientOcclusion.getHeight() / 2);

		occlusionOffX = ambientOcclusion.getOriginX();
		occlusionOffY = ambientOcclusion.getOriginY();
	}

	public Sprite getFacet () {
		return facet;
	}

	public OrthographicAlignedStillModel getCarStillModel () {
		return carStillModel;
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

	public void renderShadows (SpriteBatch batch, EntityRenderState state) {
		ambientOcclusion.setPosition(state.position.x - occlusionOffX, state.position.y - occlusionOffY);
		ambientOcclusion.setRotation(state.orientation);
		ambientOcclusion.draw(batch, 0.75f * alpha);
	}

	public void render (SpriteBatch batch, EntityRenderState state) {
		facet.setPosition(state.position.x - facetOffX, state.position.y - facetOffY);
		facet.setRotation(state.orientation);

		facet.draw(batch, alpha);
	}
}
