
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
	private int nearestWp = -1, prevNearestWp = -1, nextNearestWp = -1;

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
		nearestWp = -1;
		prevNearestWp = -1;
		nextNearestWp = -1;
	}

	private Vector2 tmprj = new Vector2();

	public void update () {
		if (car == null) {
			return;
		}

		updateDistances();
		updateNearestWaypoints();

		if (nearestWp > -1 && nextNearestWp > -1 && prevNearestWp > -1) {
			project(route.get(nearestWp), route.get(prevNearestWp), car.getWorldPosMt());
			tmprj.set(prj);
			project(route.get(nearestWp), route.get(nextNearestWp), car.getWorldPosMt());
			tmprj.lerp(prj, 0.5f);
			prj.set(tmprj);
		}

// float d = route.get(nearestWp).crs(car.getWorldPosMt());
// Gdx.app.log("RouteTracker", "d=" + d);

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

		if (nearest != nearestWp) {
			prevNearestWp = nearestWp;
			nearestWp = nearest;
		}

		if (nearestWp == -1 || prevNearestWp == -1) {
			return;
		}

		// compute next nearest
		boolean forward = (nearestWp > prevNearestWp) || (nearestWp == 0 && prevNearestWp == route.size() - 1);
		forward &= !(nearestWp == route.size() - 1 && prevNearestWp == 0);

		if (forward) {
			nextNearestWp = nearestWp + 1;
			if (nextNearestWp >= route.size()) {
				nextNearestWp = 0;
			}
		} else {
			nextNearestWp = nearestWp - 1;
			if (nextNearestWp < 0) {
				nextNearestWp = route.size() - 1;
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

	private Vector2 prj = new Vector2(0, 0);

	private Vector2 project (Vector2 line1, Vector2 line2, Vector2 toProject) {
		float m = (line2.y - line1.y) / (line2.x - line1.x);
		float b = line1.y - (m * line1.x);

		float x = (m * toProject.y + toProject.x - m * b) / (m * m + 1);
		float y = (m * m * toProject.y + m * toProject.x + b) / (m * m + 1);

		prj.set(x, y);
		return prj;
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
			if (i == nearestWp) {
				// nearest
				shape.setColor(1f, 0.2f, 0.2f, 0.85f);
				shape.filledCircle(p.x, p.y, 0.8f, 100);
			} else if (i == prevNearestWp) {
				// previous nearest
				shape.setColor(1f, 0.5f, 0.2f, 0.5f);
				shape.filledCircle(p.x, p.y, 0.65f, 100);
			} else if (i == nextNearestWp) {
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

		// prj dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(1, 1, 1, 1);
		shape.filledCircle(prj.x, prj.y, 0.25f, 100);
		shape.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
}
