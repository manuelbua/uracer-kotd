
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import net.sourceforge.jFuzzyLogic.FIS;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines.EngineF40;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.engines.EngineSoundSet;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.utils.AMath;

public final class PlayerEngineSoundEffect extends SoundEffect {

	// inference engine
	private FIS feLoad;
	private float load;
	private float throttle;

	private EngineSoundSet soundset = new EngineF40();

	public PlayerEngineSoundEffect () {
		feLoad = FIS.load(Gdx.files.getFileHandle("data/audio/car-engine/fuzzy/engineLoad.fcl", FileType.Internal).read(), true);
		load = 0;
		throttle = 0;
	}

	@Override
	public void dispose () {
		stop();
	}

	private float fuzzyLoadCompute (float throttle, float rpm) {
		feLoad.setVariable("throttle", throttle);
		feLoad.setVariable("rpm", rpm);
		feLoad.evaluate();
		return (float)feLoad.getVariable("load").getValue();
	}

	@Override
	public void tick () {
		// update throttle and rpms, set them as input for the next-frame load computation
		// float throttle = player.carState.currSpeedFactor * 100;
		if (player.isThrottling) {
			// throttle = player.getSimulator().carDesc.throttle / 300 * 100;
			throttle = player.carState.currSpeedFactor * 100;
		} else {
			throttle *= AMath.damping(0.55f);
		}

		float rpm = soundset.updateRpm(load);
		load = AMath.fixup(fuzzyLoadCompute(throttle, rpm));
		Gdx.app.log("", "engine load=" + load + ", rpm=" + rpm + ", th=" + throttle + ", g=" + soundset.getGear());

		// compute volumes
		soundset.updateVolumes(load);
		soundset.updatePitches();
		soundset.updateGear(player.isThrottling);
	}

	@Override
	public void stop () {
		soundset.stop();
	}

	@Override
	public void gameReset () {
		gameRestart();
	}

	@Override
	public void gameRestart () {
		soundset.reset();
		soundset.stop();
		soundset.start();
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		soundset.setPlayer(player);

		if (hasPlayer) {
			soundset.start();
		} else {
			soundset.stop();
		}
	}
}
