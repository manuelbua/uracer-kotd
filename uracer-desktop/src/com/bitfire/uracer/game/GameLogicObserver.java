
package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.player.PlayerCar;

public interface GameLogicObserver {
	void handleExtraInput ();

	void beforeRender ();

	float updateCameraZoom (float timeModFactor);

	void updateCameraPosition (Vector2 positionPx);

	void collision (CarEvent.Data data);

	void physicsForcesReady (CarEvent.Data eventData);

	void ghostReplayStarted (GhostCar ghost);

	void ghostReplayEnded (GhostCar ghost);

	void ghostLapStarted (GhostCar ghost);

	void ghostLapCompleted (GhostCar ghost);

	void ghostFadingOut (GhostCar ghost);

	void playerLapStarted ();

	void playerLapCompleted ();

	void warmUpStarted ();

	void warmUpCompleted ();

	void driftBegins (PlayerCar player);

	void driftEnds (PlayerCar player);

	void wrongWayBegins ();

	void wrongWayEnds ();

	void outOfTrack ();

	void backInTrack ();

	void doQuit ();
}
