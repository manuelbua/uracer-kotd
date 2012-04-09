package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public abstract class Box2dEntity extends SubframeInterpolableEntity {
	protected Body body;

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( Type type ) {
			SpriteBatch batch = Events.gameRenderer.batch;

			switch( type ) {
			case BatchBeforeMeshes:
				onRender( batch );
				break;
			case BatchDebug:
				onDebug( batch );
				break;
			}
		}
	};

	public abstract void onRender( SpriteBatch batch );
	public abstract void onDebug( SpriteBatch batch );

	public Box2dEntity() {
		Events.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, GameRendererEvent.Order.DEFAULT );
		Events.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT );
	}

//	public Box2dEntity(GameRendererEvent.Order orderForBatchBeforeMeshes, GameRendererEvent.Order orderForDebug) {
//		GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, orderForBatchBeforeMeshes );
//		GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug, orderForDebug );
//	}

	@Override
	public void dispose() {
		GameData.b2dWorld.destroyBody( body );
	}

	public Body getBody() {
		return body;
	}

	@Override
	public void saveStateTo( EntityState state ) {
		state.position.set( body.getPosition() );
		state.orientation = body.getAngle();
	}

	@Override
	public boolean isSubframeInterpolated() {
		return Config.Graphics.SubframeInterpolation;
	}

	@Override
	public void onBeforePhysicsSubstep() {
		toNormalRelativeAngle();
		super.onBeforePhysicsSubstep();
	}

	private Vector2 _pos = new Vector2();

	public Vector2 pos() {
		_pos.set( Convert.mt2px( body.getPosition() ) );
		return _pos;
	}

	public void pos( Vector2 pos ) {
		setTransform( pos, orient() );
	}

	public float orient() {
		return -body.getAngle() * MathUtils.radiansToDegrees;
	}

	public void orient( float orient ) {
		setTransform( pos(), orient );
	}

	private Vector2 tmp = new Vector2();

	public void setTransform( Vector2 position, float orient ) {
		tmp.set( Convert.px2mt( position ) );
		body.setTransform( tmp, -orient * MathUtils.degreesToRadians );
		toNormalRelativeAngle();
		resetState();
	}

	protected void toNormalRelativeAngle() {
		// normalize body angle since it can grows unbounded
		float angle = AMath.normalRelativeAngle( body.getAngle() );
		body.setTransform( body.getPosition(), angle );
	}
}
