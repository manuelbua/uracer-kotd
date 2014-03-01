
package com.bitfire.uracer.game;

import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayResult;
import com.bitfire.uracer.game.logic.types.helpers.TimeModulator;

public interface GameLogic {
	void dispose ();

	void addPlayer ();

	void removePlayer ();

	void restartGame ();

	void resetGame ();

	void quitGame ();

	void pauseGame ();

	void resumeGame ();

	boolean isQuitPending ();

	boolean isPaused ();

	float getCollisionFactor ();

	float getCollisionFrontRatio ();

	void endCollisionTime ();

	GhostCar getNextTarget ();

	void startTimeDilation ();

	void endTimeDilation ();

	boolean isTimeDilationAvailable ();

	void tick ();

	void tickCompleted ();

	Time getOutOfTrackTimer ();

	Time getTimeDilationTimer ();

	TimeModulator getTimeModulator ();

	GhostCar[] getGhosts ();

	GhostCar getGhost (int handle);

	boolean isGhostActive (int handle);

	boolean isWarmUp ();

	boolean hasPlayer ();

	UserProfile getUserProfile ();

	ReplayResult getLastRecordedInfo ();

	void showMessage (String message, float durationSecs, Message.Type type, Position position, Size size);

	void chooseNextTarget (boolean backward);
}
