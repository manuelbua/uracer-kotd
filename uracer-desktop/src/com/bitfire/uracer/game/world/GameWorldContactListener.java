
package com.bitfire.uracer.game.world;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.bitfire.uracer.game.collisions.CarImpactManager;

public class GameWorldContactListener implements ContactListener {
	private CarImpactManager impactManager = new CarImpactManager();

	@Override
	public void beginContact (Contact contact) {
		// if(Box2DUtils.isCar(contact.getFixtureA()) || Box2DUtils.isCar(contact.getFixtureB()))
		// System.out.println("beginContact");
		// Gdx.app.log("Cntct", "begin");
	}

	@Override
	public void endContact (Contact contact) {
		// if(Box2DUtils.isCar(contact.getFixtureA()) || Box2DUtils.isCar(contact.getFixtureB()))
		// System.out.println("endContact");
		// Gdx.app.log("Cntct", "end");
	}

	@Override
	public void preSolve (Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve (Contact contact, ContactImpulse impulse) {
		impactManager.process(contact, impulse);
	}
}
