
package com.bitfire.uracer.game.logic.helpers;

import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudLapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayerStatic;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerDriftSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerImpactSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSkidMarks;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSmokeTrails;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.player.PlayerCar;

/** Manages the creation and destruction of the player-bound game tasks. */
public final class PlayerGameTasks {

	private final UserProfile userProfile;
	private final GameTasksManager manager;

	/** keeps track of the concrete player tasks (note that they are all publicly accessible for performance reasons) */

	public HudPlayer hudPlayer = null;
	public HudPlayerStatic hudPlayerStatic = null;
	public HudLapInfo hudLapInfo = null;
	public PlayerSkidMarks playerSkidMarks = null;
	public PlayerSmokeTrails playerSmokeTrails = null;
	public PlayerDriftSoundEffect playerDriftSoundFx = null;
	public PlayerImpactSoundEffect playerImpactSoundFx = null;

	// public PlayerEngineSoundEffect playerEngineSoundFx = null;

	public PlayerGameTasks (UserProfile userProfile, GameTasksManager gameTaskManager) {
		this.userProfile = userProfile;
		manager = gameTaskManager;
	}

	public void dispose () {
		destroyTasks();
	}

	public void createTasks (LapManager lapManager) {
		// sounds
		playerDriftSoundFx = new PlayerDriftSoundEffect();
		playerImpactSoundFx = new PlayerImpactSoundEffect();
		manager.sound.add(playerDriftSoundFx);
		manager.sound.add(playerImpactSoundFx);
		// playerEngineSoundFx = new PlayerEngineSoundEffect(player);
		// manager.sound.add(playerEngineSoundFx);

		// track effects
		int maxSkidMarks = URacer.Game.isDesktop() ? 150 : 100;
		float maxLife = URacer.Game.isDesktop() ? 5 : 3;
		playerSkidMarks = new PlayerSkidMarks(maxSkidMarks, maxLife);
		playerSmokeTrails = new PlayerSmokeTrails();
		manager.effects.addBeforeCars(playerSkidMarks);
		manager.effects.addAfterCars(playerSmokeTrails);

		// hud
		hudPlayer = new HudPlayer(userProfile);
		hudPlayerStatic = new HudPlayerStatic(userProfile);
		hudLapInfo = new HudLapInfo(lapManager);

		manager.hud.addBeforePostProcessing(hudPlayer);
		manager.hud.addAfterPostProcessing(hudLapInfo);
		manager.hud.addAfterPostProcessing(hudPlayerStatic);
	}

	public void destroyTasks () {
		manager.sound.disposeTasks();
		manager.effects.disposeTasks();
		manager.hud.disposeTasks();
		if (manager.debug != null) manager.debug.disposeTasks();
	}

	public void playerAdded (PlayerCar player) {
		manager.sound.onPlayerSet(player);
		manager.effects.onPlayerSet(player);
		manager.hud.onPlayerSet(player);
		if (manager.debug != null) manager.debug.onPlayerSet(player);
	}

	public void playerRemoved () {
		manager.sound.onPlayerSet(null);
		manager.effects.onPlayerSet(null);
		manager.hud.onPlayerSet(null);
		if (manager.debug != null) manager.debug.onPlayerSet(null);
	}
}
