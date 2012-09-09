
package com.bitfire.uracer.game.logic.helpers;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;

public final class RouteTracker implements Disposable, GameRendererEvent.Listener {

	// dbg render
	private final GameWorldRenderer gameWorldRenderer;
	private final ShapeRenderer shape = new ShapeRenderer();
	private Car car;
	private int carSector;

	private final List<Vector2> route;
	private final List<Polygon> polys;
	private final TrackSector[] sectors;
	private final float totalLength;

	Vector2 tmp = new Vector2();

	private static class TrackSector {
		public final Polygon poly;
		public final float length;
		public final float relativeTotal;
		public final Vector2 leading, trailing;

		public TrackSector (Polygon poly, float length, float relativeTotalLength, Vector2 leading, Vector2 trailing) {
			this.poly = poly;
			this.length = length;
			this.relativeTotal = relativeTotalLength;
			this.leading = leading;
			this.trailing = trailing;
		}
	}

	public RouteTracker (GameWorld gameWorld, GameWorldRenderer gameWorldRenderer) {
		this.route = gameWorld.getTrackRoute();
		this.polys = gameWorld.getTrackPolygons();
		this.gameWorldRenderer = gameWorldRenderer;
		this.sectors = new TrackSector[route.size()];

		totalLength = sectorize();
		Gdx.app.log("RouteTracker", "total length = " + totalLength);

		GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
	}

	private float sectorize () {
		float accuLength = 0;
		for (int i = 0; i < route.size(); i++) {
			Vector2 from = route.get(i);
			Vector2 to;

			if (i == route.size() - 1) {
				to = route.get(0);
			} else {
				to = route.get(i + 1);
			}

			float len = from.dst2(to);

			int p = findPolygon(from, to);
			if (p == -1) {
				throw new GdxRuntimeException("Cannot find a matching sectors for points (" + (i - 1) + "," + i);
			}

			TrackSector ts = new TrackSector(polys.get(p), len, accuLength, from, to);
			sectors[i] = ts;

			Gdx.app.log("RouteTracker::sectorizer", from + " -> " + to + ", poly=" + p + ", len=" + len);

			accuLength += len;
		}

		return accuLength;

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
	}

	public void update () {
		if (car == null) {
			return;
		}

		Vector2 pt = car.getWorldPosMt();
		carSector = findSector(pt);
		if (carSector != -1) {
			TrackSector s = sectors[carSector];
			float carlen = (s.relativeTotal + s.length * distInSector(s, pt));
			float trackPercent = (carlen / totalLength) * 100;
			Gdx.app.log("RouteTracker", "tracklen=" + carlen + ", track_completion=" + Math.round(trackPercent) + "%");
		}
	}

	private Vector2 vlp = new Vector2();
	private Vector2 vtp = new Vector2();
	private Vector2 norm = new Vector2();

	private float distInSector (TrackSector sector, Vector2 p) {
		float fdl, fdt;

		vlp.x = p.x - sector.leading.x;
		vlp.y = p.y - sector.leading.y;

		vtp.x = p.x - sector.trailing.x;
		vtp.y = p.y - sector.trailing.y;

		norm.set(sector.leading).sub(sector.trailing).nor();
		fdl = vlp.x * norm.x + vlp.y * norm.y;

		norm.set(sector.trailing).sub(sector.leading).nor();
		fdt = vtp.x * norm.x + vtp.y * norm.y;

		return fdl / (fdl + fdt);
	}

	private Vector2 tmprj = new Vector2();

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

	private int findSector (Vector2 point) {
		for (int i = 0; i < sectors.length; i++) {
			TrackSector s = sectors[i];
			Polygon p = s.poly;
			if (p.contains(point.x, point.y)) {
				return i;
			}
		}

		return -1;
	}

	private int findPolygon (Vector2 a, Vector2 b) {
		for (int i = 0; i < polys.size(); i++) {
			Polygon p = polys.get(i);
			p.scale(0.1f);
			if (p.contains(a.x, a.y) && p.contains(b.x, b.y)) {
				p.scale(-0.1f);
				return i;
			}

			p.scale(-0.1f);
		}

		return -1;
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
			shape.setColor(0.8f, 1, 0.9f, 0.5f);
			shape.filledCircle(p.x, p.y, 0.5f, 100);
		}
		shape.end();

		// car dot
		shape.begin(ShapeType.FilledCircle);
		shape.setColor(1f, 1, 0.45f, 0.5f);
		shape.filledCircle(car.getBody().getPosition().x, car.getBody().getPosition().y, 0.5f, 100);
		shape.end();

		// sectors
		shape.begin(ShapeType.Line);
		for (int i = 0; i < sectors.length; i++) {
			Polygon p = sectors[i].poly;

			if (i == carSector) {
				continue;
			}

			shape.setColor(1, 1, 1, 1f);
			drawSector(sectors[i]);
		}

		// car sector
		if (carSector > -1) {
			shape.setColor(1, 0, 0, 1f);
			drawSector(sectors[carSector]);
		}

		shape.end();

		// car sector's dots
		if (carSector > -1) {
			TrackSector s = sectors[carSector];

			shape.begin(ShapeType.FilledCircle);

			shape.setColor(1f, 0, 0f, 0.85f);
			shape.filledCircle(s.leading.x, s.leading.y, 0.5f, 100);
			shape.filledCircle(s.trailing.x, s.trailing.y, 0.5f, 100);

			shape.end();
		}

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	private void drawSector (TrackSector sector) {
		Polygon p = sector.poly;
		float[] vertices = p.getTransformedVertices();
		shape.line(vertices[0], vertices[1], vertices[2], vertices[3]);
		shape.line(vertices[2], vertices[3], vertices[4], vertices[5]);
		shape.line(vertices[4], vertices[5], vertices[6], vertices[7]);
		shape.line(vertices[6], vertices[7], vertices[0], vertices[1]);
	}
}
