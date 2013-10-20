
package com.bitfire.uracer.game.collisions;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarType;

/** Manages to distinguish and filter out Car-to-<entity> collisions only, raising the associated events on the correct entities.
 * 
 * @author bmanuel */
public class CarImpactManager extends ImpactManager {
	private Vector2 tmpVec2 = new Vector2();

	@Override
	public void process (Contact contact, ContactImpulse impulse) {
		Fixture a = contact.getFixtureA();
		Fixture b = contact.getFixtureB();

		ifCarThenCollide(contact, a, b, impulse);
		ifCarThenCollide(contact, b, a, impulse);
	}

	private void ifCarThenCollide (Contact contact, Fixture f, Fixture other, ContactImpulse impulse) {
		Body body = f.getBody();
		Object userData = f.getUserData();
		if ((body != null) && (userData == CarType.PlayerCar || userData == CarType.ReplayCar)) {
			Car car = (Car)body.getUserData();
			float[] impulses = impulse.getNormalImpulses();
			tmpVec2.set(impulses[0], impulses[1]);

			// dbg
			Fixture fcar = null;
			if (contact.getFixtureA().getUserData() == CarType.PlayerCar) {
				fcar = contact.getFixtureA();
			} else if (contact.getFixtureB().getUserData() == CarType.PlayerCar) {
				fcar = contact.getFixtureB();
			}

			// assumes perfect side collision
			float front_ratio = 0.5f;

			// compute median front/rear ratio for collision points
			if (fcar != null) {
				front_ratio = 0;
				float ml = car.getCarModel().length;
				float half_ml = ml * 0.5f;
				Vector2[] pts = contact.getWorldManifold().getPoints();

				int num_points = contact.getWorldManifold().getNumberOfContactPoints();
				for (int i = 0; i < num_points; i++) {
					Vector2 lp = fcar.getBody().getLocalPoint(pts[i]);

					// examine front/rear ratio
					float r = MathUtils.clamp(lp.y + half_ml, 0, ml);
					r /= ml;
					front_ratio += r;
				}

				front_ratio /= (float)num_points;
				// Gdx.app.log("Cntct", "" + front_ratio);
			}
			// dbg

			car.onCollide(other, tmpVec2, front_ratio);
		}
	}
}
