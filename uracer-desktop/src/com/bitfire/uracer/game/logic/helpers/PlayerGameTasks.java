
package com.bitfire.uracer.game.logic.helpers;

import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.debug.HudDebug;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudLapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerDriftSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerEngineSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerImpactSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSkidMarks;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSmokeTrails;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;

/** Manages the creation and destruction of the player-bound game tasks. */
public final class PlayerGameTasks {

	private final UserProfile userProfile;
	private final GameTasksManager manager;
	private final ScalingStrategy scalingStrategy;

	/** keeps track of the concrete player tasks (note that they are all publicly accessible for performance reasons) */

	public HudPlayer hudPlayer = null;
	public HudLapInfo hudLapInfo = null;
	public HudDebug hudDebug = null;
	public PlayerSkidMarks playerSkidMarks = null;
	public PlayerSmokeTrails playerTrails = null;
	public PlayerDriftSoundEffect playerDriftSoundFx = null;
	public PlayerImpactSoundEffect playerImpactSoundFx = null;
	public PlayerEngineSoundEffect playerEngineSoundFx = null;

	public PlayerGameTasks (UserProfile userProfile, GameTasksManager gameTaskManager, ScalingStrategy strategy) {
		this.userProfile = userProfile;
		manager = gameTaskManager;
		scalingStrategy = strategy;
	}

	public void dispose () {

	}

	public void createTasks (PlayerCar player, LapInfo lapInfo, GameRenderer renderer) {
		// sounds
		playerDriftSoundFx = new PlayerDriftSoundEffect(player);
		playerImpactSoundFx = new PlayerImpactSoundEffect(player);
		playerEngineSoundFx = new PlayerEngineSoundEffect(player);

		// track effects
		int maxSkidMarks = Config.isDesktop ? 500 : 100;
		float maxLife = Config.isDesktop ? 5 : 3;
		playerSkidMarks = new PlayerSkidMarks(player, maxSkidMarks, maxLife);
		playerTrails = new PlayerSmokeTrails(scalingStrategy, player);

		// hud, player's information
		hudPlayer = new HudPlayer(userProfile, scalingStrategy, player, renderer);

		// hud, player's lap info
		hudLapInfo = new HudLapInfo(scalingStrategy, lapInfo);

		manager.sound.add(playerDriftSoundFx);
		manager.sound.add(playerImpactSoundFx);
		manager.sound.add(playerEngineSoundFx);
		manager.effects.add(playerSkidMarks);
		manager.effects.add(playerTrails);
		manager.hud.addBeforePostProcessing(hudPlayer);
		manager.hud.addBeforePostProcessing(hudLapInfo);

		// hud-style debug information for various data (player's drift state, number of skid marks particles, ..)
		if (Config.Debug.RenderHudDebugInfo) {
			hudDebug = new HudDebug(player, player.driftState, playerSkidMarks /* can be null */);
			manager.hud.addBeforePostProcessing(hudDebug);
		}
	}

	public void destroyTasks () {
		if (playerDriftSoundFx != null) {
			manager.sound.remove(playerDriftSoundFx);
			playerDriftSoundFx = null;
		}

		if (playerImpactSoundFx != null) {
			manager.sound.remove(playerImpactSoundFx);
			playerImpactSoundFx = null;
		}

		if (playerEngineSoundFx != null) {
			manager.sound.remove(playerEngineSoundFx);
			playerEngineSoundFx = null;
		}

		if (playerSkidMarks != null) {
			manager.effects.remove(playerSkidMarks);
			playerSkidMarks = null;
		}

		if (playerTrails != null) {
			manager.effects.remove(playerTrails);
			playerTrails = null;
		}

		if (hudPlayer != null) {
			manager.hud.remove(hudPlayer);
			hudPlayer = null;
		}

		if (hudLapInfo != null) {
			manager.hud.remove(hudLapInfo);
			hudLapInfo = null;
		}

		if (hudDebug != null) {
			manager.hud.remove(hudDebug);
			hudDebug = null;
		}
	}
}
