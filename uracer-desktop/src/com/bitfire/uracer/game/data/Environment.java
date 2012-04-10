package com.bitfire.uracer.game.data;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.GameWorld;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.hud.HudLabel;
import com.bitfire.uracer.game.hud.HudLabelAccessor;
import com.bitfire.uracer.game.messager.Message;
import com.bitfire.uracer.game.messager.MessageAccessor;
import com.bitfire.uracer.game.messager.Messager;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

public final class Environment {

	public ScalingStrategy scalingStrategy;
	public GameplaySettings gameSettings;
	public GameWorld gameWorld;
	public Messager messager;
	public World b2dWorld;

	public Environment( GameDifficulty difficulty ) {
		// computed for a 256px tile size target (compute needed conversion factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (scalingStrategy.targetScreenRatio / scalingStrategy.to256);

		gameSettings = new GameplaySettings( difficulty );

		// FIXME, Physics?
		b2dWorld = new World( new Vector2( 0, 0 ), false );
		b2dWorld.setContactListener( new GameContactListener() );

		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );

		messager = new Messager( scalingStrategy.invTileMapZoomFactor );
	}

	public void dispose() {
		messager.dispose();

		if( gameWorld != null ) {
			gameWorld.dispose();
		}

		if( b2dWorld != null ) {
			b2dWorld.dispose();
		}
	}

	public void createWorld( World b2dWorld, ScalingStrategy strategy, String levelName, boolean nightMode ) {
		gameWorld = new GameWorld( b2dWorld, strategy, levelName, nightMode );
	}
}