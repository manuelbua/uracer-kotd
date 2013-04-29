
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.utils.AMath;

public abstract class Box2DEntity extends SubframeInterpolableEntity {
	protected Body body;
	protected World box2dWorld;

	public void onDebug (SpriteBatch batch) {
	}

	public Box2DEntity (World world) {
		super();
		this.box2dWorld = world;
	}

	@Override
	public void dispose () {
		super.dispose();
		box2dWorld.destroyBody(body);
	}

	public Body getBody () {
		return body;
	}

	@Override
	public void saveStateTo (EntityRenderState state) {
		state.position.set(body.getPosition());
		state.orientation = body.getAngle();
	}

	@Override
	public boolean isSubframeInterpolated () {
		return Config.Graphics.SubframeInterpolation;
	}

	@Override
	public void onBeforePhysicsSubstep () {
		toNormalRelativeAngle();
		super.onBeforePhysicsSubstep();
	}

	public Vector2 getWorldPosMt () {
		return body.getPosition();
	}

	public float getWorldOrientRads () {
		return body.getAngle();
	}

	public void setWorldPosMt (Vector2 worldPosition) {
		body.setTransform(worldPosition, body.getAngle());
		resetState();
	}

	public void setWorldPosMt (Vector2 worldPosition, float orientationRads) {
		body.setTransform(worldPosition, orientationRads);
		resetState();
	}

	protected void toNormalRelativeAngle () {
		// normalize body angle since it can grows unbounded
		float angle = AMath.normalRelativeAngle(body.getAngle());
		body.setTransform(body.getPosition(), angle);
	}
}
