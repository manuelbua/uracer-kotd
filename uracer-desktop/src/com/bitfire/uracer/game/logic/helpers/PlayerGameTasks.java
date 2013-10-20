
package com.bitfire.uracer.game.logic.helpers;

import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudLapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayerStatic;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerDriftSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerEngineSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerImpactSoundEffect;
import com.bitfire.uracer.game.logic.gametasks.sounds.effects.PlayerTensiveMusic;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSkidMarks;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.effects.PlayerSmokeTrails;
import com.bitfire.uracer.game.logic.replaying.LapManager;

/** Manages the creation and destruction of the player-bound game tasks. */
public final class PlayerGameTasks {

	private final UserProfile userProfile;
	private final GameTasksManager manager;

	/** keeps track of the concrete player tasks (note that they are all publicly accessible for performance reasons) */

	public HudPlayer hudPlayer = null;
	public HudPlayerStatic hudPlayerStatic = null;
	public HudLapInfo hudLapInfo = null;

	public PlayerGameTasks (UserProfile userProfile, GameTasksManager gameTaskManager) {
		this.userProfile = userProfile;
		manager = gameTaskManager;
	}

	public void dispose () {
		destroyTasks();
	}

	public void createTasks (LapManager lapManager, TrackProgressData progressData) {
		// sounds
		manager.sound.add(new PlayerDriftSoundEffect());
		manager.sound.add(new PlayerImpactSoundEffect());
		manager.sound.add(new PlayerEngineSoundEffect(progressData));
		manager.sound.add(new PlayerTensiveMusic(progressData));

		// track effects
		int maxSkidMarks = URacer.Game.isDesktop() ? 150 : 100;
		float maxLife = URacer.Game.isDesktop() ? 5 : 3;
		manager.effects.addBeforeCars(new PlayerSkidMarks(maxSkidMarks, maxLife));
		manager.effects.addAfterCars(new PlayerSmokeTrails());

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
	}
}
