package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.game.logic.GameContactListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.tiled.ScalingStrategy;
import com.bitfire.uracer.tweener.Tweener;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public class GameData {

	public static ScalingStrategy scalingStrategy;

	public static Tweener tweener;
	public static World world;
	public static Hud hud;

	// state
	public static LapState lapState;
	public static PlayerState playerState;
	public static DriftState driftState;

	public static void create() {
		// computed for a 256px tile size target (need conversion)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );
		GameData.tweener = createTweener();
		GameData.world = createWorld();

		GameData.lapState = new LapState();
		GameData.lapState.reset();
		GameData.driftState = new DriftState();
		GameData.driftState.reset();
	}

	public void dispose() {
		GameData.world.dispose();
		GameData.hud.dispose();
		GameData.tweener.clear();
		GameData.tweener.dispose();
	}

	private static Tweener createTweener() {
		Tweener t = new Tweener();
		Tweener.registerAccessor( Message.class, new MessageAccessor() );
		Tweener.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		return t;
	}

	private static World createWorld() {
		World w = new World( new Vector2( 0, 0 ), false );
		w.setContactListener( new GameContactListener() );
		return w;
	}
}
