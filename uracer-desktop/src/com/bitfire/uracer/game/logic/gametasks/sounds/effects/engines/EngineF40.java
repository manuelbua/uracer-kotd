
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import com.bitfire.uracer.resources.Sounds;

public class EngineF40 extends EngineSoundSet {
	private float[] gears = {3, 1, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f};

	public EngineF40 () {
		super();
		engine = Sounds.carEngine_f40;
		rpm = 1000;
		gear = 0;
	}

	@Override
	public float getGlobalVolume () {
		return 0.2f;
	}

	@Override
	public float getGearRatio () {
		return gears[gear];
	}

	@Override
	public void updatePitches () {
		float lowLimit = 6000f;
		float hiLimit = 10000f;

		float rpmLow = rpm / lowLimit;
		float rpmDef = rpm / hiLimit;

		// sample specific
		if (rpm < lowLimit) {
			setXnaPitch(0, rpmLow);
			setXnaPitch(1, rpmDef + 0.2f);
		}

		setXnaPitch(2, rpmDef - 0.4f);
		setXnaPitch(3, rpmDef - 0.8f);
		setXnaPitch(4, rpmDef);
		setXnaPitch(5, rpmDef - 0.8f);
		setXnaPitch(6, rpmDef - 0.8f);
	}
}
