
package com.bitfire.uracer.game.logic.helpers;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;

public final class RouteTracker implements Disposable, GameRendererEvent.Listener {

	// dbg render
	private final GameWorldRenderer gameWorldRenderer;
	private final ShapeRenderer shape = new ShapeRenderer();
	private Car car;

	private final List<Vector2> route;
	private final float[] distances;
	private final float[] lengths;
	private final float totalLength;

	Vector2 tmp = new Vector2();
	private int currWp = -1, prevWp = -1, nextWp = -1;
	private Vector2 tmprj = new Vector2();
	private Vector2 prj1 = new Vector2(0, 0), prj2 = new Vector2(0, 0), prjf = new Vector2(0, 0);

	public RouteTracker (List<Vector2> route, GameWorldRenderer gameWorldRenderer) {
		this.route = route;
		this.gameWorldRenderer = gameWorldRenderer;
		this.distances = new float[route.size()];
		this.lengths = new float[route.size()];

		tmp.set(route.get(0));
		lengths[0] = 0;
		float t = 0;
		for (int i = 1; i <= route.size() - 1; i++) {
			Vector2 to = route.get(i);

			lengths[i] = tmp.dst2(to);
			t += lengths[i];
			lengths[i] += lengths[i - 1];

			Gdx.app.log("RouteTracker", tmp + " -> " + to);

			tmp.set(to);
		}

		totalLength = t;

		GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
	}

	@Override
	public void dispose () {
		GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
		shape.dispose();
	}

	public void setCar (Car car) {
		this.car = car;
	}

	public void reset () {
		currWp = -1;
		prevWp = -1;
		nextWp = -1;
	}

	private Vector2 lerped = new Vector2(), plerped = new Vector2();

	public void update () {
		if (car == null) {
			return;
		}

		updateDistances();
		updateNearestWaypoints();

		if (currWp > -1 && nextWp > -1 && prevWp > -1) {
			// car to curr
			prj1.set(project(route.get(currWp), route.get(prevWp), car.getWorldPosMt()));
			boolean prj1In = isBetween(route.get(currWp), route.get(prevWp), prj1);

			// car to next
			prj2.set(project(route.get(currWp), route.get(nextWp), car.getWorldPosMt()));
			boolean prj2In = isBetween(route.get(currWp), route.get(nextWp), prj2);

			if (prj1In) {
				prjf.set(prj1);
			} else if (prj2In) {
				prjf.set(prj2);
			} else {
// prjf.set(project(route.get(currWp), route.get(nextWp), prj1));
			}

			lerped.set(plerped);
			lerped.lerp(prjf, 0.3f);
			prjf.set(lerped);
			plerped.set(prjf);

// Gdx.app.log("RouteTracker", "prj1in=" + prj1In + ", prj2in=" + prj2In);

// if (distances[currWp] > 5) {
// prjf.set(project(route.get(currWp), route.get(nextWp), prj1));
// prjf.set(project(route.get(currWp), route.get(prevWp), prj2));
// }
		}
	}

	// compute player-to-waypoint distances
	private void updateDistances () {
		Vector2 pos = car.getWorldPosMt();
		for (int i = 0; i < route.size(); i++) {
			distances[i] = pos.dst2(route.get(i));
		}
	}

	private void updateNearestWaypoints () {
		int nearest = 0;
		float nearestD = 0;

		// compute immediate nearest
		nearestD = distances[0];
		for (int i = 1; i < route.size(); i++) {
			if (distances[i] < nearestD) {
				nearest = i;
				nearestD = distances[i];
			}
		}

		if (nearest != currWp) {
			prevWp = currWp;
			currWp = nearest;
		}

		if (currWp == -1 || prevWp == -1) {
			return;
		}

		// compute next nearest
		boolean forward = (currWp > prevWp) || (currWp == 0 && prevWp == route.size() - 1);
		forward &= !(currWp == route.size() - 1 && prevWp == 0);

		if (forward) {
			nextWp = currWp + 1;
			if (nextWp >= route.size()) {
				nextWp = 0;
			}
		} else {
			nextWp = currWp - 1;
			if (nextWp < 0) {
				nextWp = route.size() - 1;
			}
		}

		// Gdx.app.log("RouteTracker", "p=" + prevNearestWp + ", c=" + nearestWp + ",n=" + nextNearestWp);
		// Gdx.app.log("RouteTracker", "t=" + totalLength + ",c=" + lengths[nearestWp]);
// if (prevNearestWp > -1) {
// float l = lengths[prevNearestWp];
// float fromPrev = distances[prevNearestWp];
// float fromNext = distances[nextNearestWp];
// float fromCurr = distances[nearestWp];
// Gdx.app.log("RouteTracker", "t=" + totalLength + ", l=" + (l + fromPrev + fromCurr - fromNext));
// }
	}

	private Vector2 project (Vector2 line1, Vector2 line2, Vector2 toProject) {
		float m = (line2.y - line1.y) / (line2.x - line1.x);
		float b = line1.y - (m * line1.x);

		float x = (m * toProject.y + toProject.x - m * b) / (m * m + 1);
		float y = (m * m * toProject.y + m * toProject.x + b) / (m * m + 1);

		tmprj.set(x, y);
		return tmprj;
	}

	private boolean isBetween (Vector2 a, Vector2 b, Vector2 c) {
		float dotproduct = (c.x - a.x) * (b.x - a.x) + (c.y - a.y) * (b.y - a.y);
		if (dotproduct < 0) {
			return false;
		}

		float squaredlengthba = a.dst2(b);
		if (dotproduct > squaredlengthba) {
			return false;
		}

		return true;
	}

	//
	// debug render
	//

	@Override
	public void gameRendererEvent (Type type, Order order) {
		render();
	}

	private void render () {
		if (car == null) {
			return;
		}

		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shape.setProjectionMatrix(gameWorldRenderer.getOrthographicMvpMt());

		tmp.set(route.get(0));
		shape.begin(ShapeType.Line);
		for (int i = 1; i <= route.size() - 1; i++) {
			Vector2 to = route.get(i);
			shape.setColor(1, 1, 1, 1);
			shape.line(tmp.x, tmp.y, to.x, to.y);
			tmp.set(to);
		}
		shape.end();

		// draw dots
		shape.begin(ShapeType.FilledCircle);
		for (int i = 0; i < route.size(); i++) {
			Vector2 p = route.get(i);
			if (i == currWp) {
				// nearest
				shape.setColor(1f, 0.2f, 0.2f, 0.85f);
				shape.filledCircle(p.x, p.y, 0.8f, 100);
			} else if (i == prevWp) {
				// previous nearest
				shape.setColor(1f, 0.5f, 0.2f, 0.5f);
				shape.filledCircle(p.x, p.y, 0.65f, 100);
			} else if (i == nextWp) {
				// next nearest
				shape.setColor(0.2f, 0.5f, 1f, 0.5f);
				shape.filledCircle(p.x, p.y, 0.65f, 100);
			} else {
				// default
				shape.setColor(0.8f, 1, 0.9f, 0.5f);
				shape.filledCircle(p.x, p.y, 0.5f, 100);
			}

		}
		shape.end();

		// car dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(1f, 1, 0.45f, 0.5f);
		shape.filledCircle(car.getBody().getPosition().x, car.getBody().getPosition().y, 0.5f, 100);
		shape.end();

		// prj1 dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(1, 0.2f, 0.2f, 1);
		shape.filledCircle(prj1.x, prj1.y, 0.25f, 100);
		shape.end();

		// prj2 dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(0.2f, 0.5f, 1, 1);
		shape.filledCircle(prj2.x, prj2.y, 0.25f, 100);
		shape.end();

		// prjf dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(1, 1, 1, 1);
		shape.filledCircle(prjf.x, prjf.y, 0.15f, 100);
		shape.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
}
