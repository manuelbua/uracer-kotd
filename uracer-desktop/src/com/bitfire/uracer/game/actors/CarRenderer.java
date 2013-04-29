
package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.ModelFactory.ModelMesh;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;

public final class CarRenderer {
	private float alpha;

	// locally cached values
	private OrthographicAlignedStillModel carStillModel;

	public CarRenderer (CarModel model, CarPreset.Type type) {
		carStillModel = ModelFactory.create(ModelMesh.Car, 0, 0, 1);
		setAspect(model, type);
	}

	public void setAspect (CarModel model, CarPreset.Type type) {
	}

	public OrthographicAlignedStillModel getCarStillModel () {
		return carStillModel;
	}

	public void setAlpha (float alpha) {
		this.alpha = alpha;
	}

	public float getAlpha () {
		return alpha;
	}
}
