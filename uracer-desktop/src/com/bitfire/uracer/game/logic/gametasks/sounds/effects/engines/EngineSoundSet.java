
package com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines;

import net.sourceforge.jFuzzyLogic.FIS;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;

public abstract class EngineSoundSet {
	public static final int NumSamples = 7;
	protected Sound[] engine = null;
	protected long[] mid = new long[NumSamples];
	protected boolean[] started = new boolean[NumSamples];
	protected float[] volumes = new float[NumSamples];
	protected FIS feIdle, feOnLow, feOnMid, feOnHigh, feOffLow, feOffMid, feOffHigh;
	protected int gear;
	protected float rpm;
	protected boolean hasPlayer;
	protected PlayerCar player;

	public EngineSoundSet () {
		rpm = 0;
		gear = 1;

		//@off
		feIdle = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolIdle.fcl", FileType.Internal).read(), true);
		feOnLow = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnLow.fcl", FileType.Internal).read(), true);
		feOnMid= FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnMid.fcl", FileType.Internal).read(), true);
		feOnHigh = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOnHigh.fcl", FileType.Internal).read(), true);
		feOffLow = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffLow.fcl", FileType.Internal).read(), true);
		feOffMid= FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffMid.fcl", FileType.Internal).read(), true);
		feOffHigh = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineVolOffHigh.fcl", FileType.Internal).read(), true);
		//@on
	}

	public abstract void updatePitches ();

	public abstract float getGearRatio ();

	public float getGearRatioOff () {
		return getGearRatio();
	}

	public boolean hasGears () {
		return false;
	}

	public void shiftUp () {
	}

	public void shiftDown () {
	}

	public void setPlayer (PlayerCar player) {
		this.player = player;
		this.hasPlayer = (player != null);
	}

	public void start () {
		for (int i = 0; i < NumSamples; i++) {
			start(i);
		}
	}

	public void stop () {
		for (int i = 0; i < NumSamples; i++) {
			stop(i);
		}
	}

	public void reset () {
		rpm = 1000;
		gear = 1;
	}

	public float updateRpm (float load) {
		if (!hasPlayer) {
			rpm = 1000;
			return 1000;
		} else {
			// very simplicistic, arcade implementation
			rpm = (1000 + 10000 * player.carState.currSpeedFactor);
		}

		rpm = MathUtils.clamp(rpm, 1000, 10000);
		return rpm;
	}

	public void updateVolumes (float load) {
		updateVolume(0, feIdle, load, rpm);
		updateVolume(1, feOnLow, load, rpm);
		updateVolume(2, feOnMid, load, rpm);
		updateVolume(3, feOnHigh, load, rpm);
		updateVolume(4, feOffLow, load, rpm);
		updateVolume(5, feOffMid, load, rpm);
		updateVolume(6, feOffHigh, load, rpm);

		// String dbg = "";
		// for (int i = 0; i < NumTracks; i++) {
		// dbg += "#" + i + "=" + String.format("%.02f", volumes[i]) + " ";
		// }
		//
		// Gdx.app.log("EngineSoundSet", dbg);
	}

	public int getGear () {
		return gear;
	}

	public float getRpm () {
		return rpm;
	}

	public float getGlobalVolume () {
		return 0.1f;
	}

	public float[] getVolumes () {
		return volumes;
	}

	//

	private void start (int track) {
		if (started[track]) {
			return;
		}

		started[track] = true;
		mid[track] = SoundEffect.loop(engine[track], 0);
		setVolume(track, 0);
	}

	private void stop (int track) {
		if (!started[track]) {
			return;
		}

		started[track] = false;
		if (mid[track] > -1) {
			engine[track].stop(mid[track]);
		}
	}

	private float xnaToAl (float pitch) {
		return (float)Math.pow(2, pitch);
	}

	protected void setXnaPitch (int track, float pitch) {
		setPitch(track, xnaToAl(pitch));
	}

	protected void setVolume (int track, float vol) {
		engine[track].setVolume(mid[track], vol * getGlobalVolume() * SoundManager.SfxVolumeMul);
		volumes[track] = vol;
	}

	protected void setPitch (int track, float pitch) {
		engine[track].setPitch(mid[track], pitch);
	}

	private void updateVolume (int track, FIS fuzzyEngine, float load, float rpm) {
		fuzzyEngine.setVariable("load", load);
		fuzzyEngine.setVariable("rpm", rpm);
		fuzzyEngine.evaluate();
		float volume = ((float)fuzzyEngine.getVariable("volume").getValue() / 100f);

		if (volume >= 0 && volume <= 1) {
			setVolume(track, volume * SoundManager.SfxVolumeMul);

			// dbg
			// if (track == 1 || track == 4) {
			// setVolume(track, (float)volume * SoundManager.SfxVolumeMul);
			// } else {
			// setVolume(track, 0);
			// }
			// dbg
		}
	}
}
