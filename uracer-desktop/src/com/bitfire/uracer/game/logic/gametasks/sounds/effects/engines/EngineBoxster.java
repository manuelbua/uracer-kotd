
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import com.bitfire.uracer.resources.Sounds;

public class EngineBoxster extends EngineSoundSet {
	private float[] gears = {3, 1, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f};

	public EngineBoxster () {
		super();
		engine = Sounds.carEngine_a1;
		rpm = 1000;
		gear = 0;
	}

	@Override
	public float getGlobalVolume () {
		return 0.1f;
	}

	@Override
	public float getGearRatio () {
		return gears[gear];
	}

	@Override
	public void updatePitches () {
		// sample specific
		if (rpm < 5000) {
			setXnaPitch(0, rpm / 5000);
		}

		setXnaPitch(1, rpm / 10000 - 1);
		setXnaPitch(2, rpm / 10000 - 1);
		setXnaPitch(3, rpm / 10000 - 1);
		setXnaPitch(4, rpm / 10000 - 1);
		setXnaPitch(5, rpm / 10000 - 1);
		setXnaPitch(6, rpm / 10000 - 1);
	}
}
