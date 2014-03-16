
package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.player.PlayerCar;

public interface PostProcessingAnimator {
	void update (Vector2 cameraPos, TrackProgressData progressData, Color ambient, Color trees, float zoom,
		float warmUpCompletion, float collisionFactor, boolean paused);

	void alertBegins (int milliseconds);

	void alertEnds (int milliseconds);

	void gamePause (int milliseconds);

	void gameResume (int milliseconds);

	public void alert (int milliseconds);

	void reset ();

	void setPlayer (PlayerCar player);
}
