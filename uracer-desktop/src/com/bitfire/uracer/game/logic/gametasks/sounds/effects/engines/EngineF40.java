
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.InterpolatedFloat;

public class EngineF40 extends EngineSoundSet {
	private static final boolean UseGears = true;
	private static final int MinGear = 1;
	private static final int MaxGear = 3;
	private InterpolatedFloat irpm = new InterpolatedFloat(), speed = new InterpolatedFloat();

	// private FIS autoGears;

	public EngineF40 () {
		super();
		engine = Sounds.carEngine_f40;
		rpm = 1000;
		gear = MinGear;

		// autoGears = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/autoGears.fcl", FileType.Internal).read(),
		// true);
	}

	@Override
	public float getGlobalVolume () {
		float vol = 0.1f;
		// if (hasPlayer) {
		// vol += 0.2f * player.carState.currSpeedFactor;
		// }

		return vol;
	}

	@Override
	public boolean hasGears () {
		return UseGears;
	}

	@Override
	public float getGearRatio () {
		if (!UseGears) {
			return 3;
		}

		// {3, 1, 0.7f, 0.5f, 0.3f, 0.2f, 0.1f}

		float res = 1;

		//@off
		switch(gear) {
		case 0: res =  1f;
		//
		case 1: res = 1.2f; 	break;
		case 2: res = 0.7f;	break;
		case 3: res = 0.55f;	break;
		case 4: res = 0.2f;	break;
		case 5: res = 0.3f;	break;
		case 6: res = 0.2f;	break;
		}
		//@on

		return res;// * 0.15f;
	}

	@Override
	public float getGearRatioOff () {
		if (!UseGears) {
			return 3;
		}

		float res = 1;

		//@off
		switch(gear) {
		case 0: res =  1f;
		//
		case 1: res = 1f; 	break;
		case 2: res = 0.8f;	break;
		case 3: res = 0.7f;	break;
		case 4: res = 0f;	break;
		case 5: res = 0f;	break;
		case 6: res = 0f;	break;
		}
		//@on

		return res;// * 0.15f;
	}

	@Override
	public void updatePitches () {
		float lowLimit = 6000f;
		float hiLimit = 10000f;

		// rpm = 1000;
		float rpmLow = rpm / lowLimit;
		float rpmDef = rpm / hiLimit;

		// dbg
		// setVolume(1, 1 * SoundManager.SfxVolumeMul);
		// setVolume(4, 1 * SoundManager.SfxVolumeMul);
		// dbg

		// sample specific
		if (rpm < lowLimit) {
			setXnaPitch(0, rpmLow);
			setXnaPitch(1, rpmDef + 0.2f);
		}

		// setXnaPitch(2, rpmDef - 0.4f);
		// setXnaPitch(3, rpmDef - 0.8f);
		// setXnaPitch(4, rpmDef);
		// setXnaPitch(5, rpmDef - 0.8f);
		// setXnaPitch(6, rpmDef - 0.8f);

		setXnaPitch(2, rpmDef - 0.4f);
		setXnaPitch(3, rpmDef - 0.8f);
		setXnaPitch(4, rpmDef + 0.2f);
		setXnaPitch(5, rpmDef - 0.8f);
		setXnaPitch(6, rpmDef - 0.8f);

	}

	private float prevSpeed = 0;

	@Override
	public float updateRpm (float load) {
		if (UseGears) {
			if (!hasPlayer) {
				rpm = 1000;
				return 1000;
			}

			updateGear();
			speed.set(player.carState.currSpeedFactor, 0.85f);
			float sf = speed.get();

			float q = 15000;// 12858;
			float factor = q * sf * getGearRatio();

			// updateGearFIS();
			if (sf < prevSpeed) {
				factor = q * sf * getGearRatioOff();
			}

			Gdx.app.log("EngineSoundSet", "gear=" + gear + ", rpm=" + rpm + ", throttle=" + player.getCarDescriptor().throttle
				+ ", throttling=" + player.isThrottling);// + ", speed="+ sf);

			// more accurate representation
			// float inc = 0;
			// if (load < 0) {
			// inc = load;
			// } else {
			// inc = load * getGearRatio() * sf;
			// }
			// float newrpm = rpm + inc;

			float newrpm = 1000 + factor + (load < 0 ? load * 10f : load * 10);
			irpm.set(newrpm, 0.7f);
			newrpm = irpm.get();

			// rpm = AMath.lerp(rpm, newrpm, 1);
			// float newrpm = rpm + (load < 0 ? load : load * getGearRatio());

			// float rpmw = player.getSimulator().getRpmWheel();
			// float newrpm = rpm + rpmw * getGearRatio() * 100f + (load < 0 ? load * 4 : load * sf * 0.001f);
			// float newrpm = rpmw * getGearRatio() * 3f;

			newrpm = MathUtils.clamp(newrpm, 1000, 10000);

			rpm = newrpm;

			// Gdx.app.log("EngineSoundSet", "gear=" + gear + ", rpm=" + rpm + ", throttle=" + player.getCarDescriptor().throttle
			// + ", throttling=" + player.isThrottling);// + ", speed="+ sf);

			rpm = MathUtils.clamp(rpm, 1000, 10000);

			prevSpeed = sf;
			return rpm;
		}

		return super.updateRpm(load);
	}

	// private void updateGearFIS () {
	// autoGears.setVariable("SPEED", player.carState.currSpeedFactor * 126);
	// autoGears.setVariable("CS", 0);
	// autoGears.evaluate();
	// int newgear = (int)(autoGears.getVariable("GEAR").getValue() + 0.5);
	//
	// if (newgear > gear) {
	// shiftUp();
	// } else if (newgear < gear) {
	// shiftDown();
	// }
	//
	// }

	private int updateGear () {
		float sf = player.carState.currSpeedFactor;

		if (sf > prevSpeed) {
			switch (gear) {
			case 1:
				if (rpm > 9500) {
					shiftUp();
					return 1;
				}
				break;
			case 2:
				if (rpm > 9500) {
					shiftUp();
					return 1;
				}
				break;
			// case 3:
			// if (rpm > 9000) {shiftUp();return 1;}
			// break;
			}

		} else if (sf < prevSpeed) {

			switch (gear) {
			// case 4:
			// if (rpm < 8000) {shiftDown();return -1;}
			// break;
			case 3:
				if (rpm < 8000) {
					shiftDown();
					return -1;
				}
				break;
			case 2:
				if (rpm < 2000) {
					shiftDown();
					return -1;
				}
				break;
			}
		}

		return 0;
	}

	@Override
	public void shiftUp () {
		if (UseGears) {
			if (gear > 0 && gear < MaxGear) {
				gear++;

				float dist = 2000;
				if (rpm >= dist + 1000) {
					// rpm -= dist;
				} else {
					// rpm = 1000;
				}
			}

			if (gear == 0) {
				gear++;
				// rpm = 1000;
			}

			rpm = MathUtils.clamp(rpm, 1000, 10000);
		}

		gear = MathUtils.clamp(gear, MinGear, MaxGear);
	}

	@Override
	public void shiftDown () {
		if (UseGears) {
			// gear = 1;

			// if (gear == 1) {
			// gear--;
			// }

			if (gear > MinGear && gear <= MaxGear) {
				gear--;
				if (rpm != 1000) {
					// rpm += MathUtils.random(-500, 1100);
					// rpm += 3000;
				}

				rpm = MathUtils.clamp(rpm, 1000, 10000);
				// Gdx.app.log("EngineF40", "shift down");
			}
		}

		gear = MathUtils.clamp(gear, MinGear, MaxGear);
	}

	@Override
	public void reset () {
		super.reset();

		rpm = 1000;
		gear = 1;
		irpm.reset(true);
		speed.reset(true);
	}
}
