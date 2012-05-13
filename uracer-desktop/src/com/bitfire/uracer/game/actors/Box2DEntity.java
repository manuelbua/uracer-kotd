package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Type;
import com.bitfire.uracer.utils.AMath;

public abstract class Box2DEntity extends SubframeInterpolableEntity {
	protected Body body;
	protected World box2dWorld;
	private GameRendererEvent.Order drawingOrder;

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( Type type ) {
			onRender( GameEvents.gameRenderer.batch );
		}
	};

	public abstract void onRender( SpriteBatch batch );

	public void onDebug( SpriteBatch batch ) {
	}

	public Box2DEntity( World world, GameRendererEvent.Order drawingOrder ) {
		super();
		this.box2dWorld = world;
		this.drawingOrder = drawingOrder;
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, drawingOrder );
		// GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug,
		// GameRendererEvent.Order.DEFAULT );
	}

	// public Box2DEntity(GameRendererEvent.Order orderForBatchBeforeMeshes, GameRendererEvent.Order orderForDebug) {
	// GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
	// orderForBatchBeforeMeshes );
	// GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug, orderForDebug );
	// }

	@Override
	public void dispose() {
		super.dispose();
		GameEvents.gameRenderer.removeListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, drawingOrder );
		box2dWorld.destroyBody( body );
	}

	public Body getBody() {
		return body;
	}

	@Override
	public void saveStateTo( EntityRenderState state ) {
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

	public Vector2 getWorldPosMt() {
		return body.getPosition();
	}

	public float getWorldOrientRads() {
		return body.getAngle();
	}

	public void setWorldPosMt(Vector2 worldPosition) {
		body.setTransform( worldPosition, body.getAngle() );
	}

	public void setWorldPosMt(Vector2 worldPosition, float orientationRads) {
		body.setTransform( worldPosition, orientationRads );
	}

	protected void toNormalRelativeAngle() {
		// normalize body angle since it can grows unbounded
		float angle = AMath.normalRelativeAngle( body.getAngle() );
		body.setTransform( body.getPosition(), angle );
	}
}
