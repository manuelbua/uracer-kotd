
package com.bitfire.uracer.game.collisions;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;

/** Represents an entity being able to filter and analyze contacts, detecting and producing the correct events, dispatching them to
 * the respective entities.
 * 
 * @author bmanuel */
public abstract class ImpactManager {
	abstract void process (Contact contact, ContactImpulse impulse);
}
