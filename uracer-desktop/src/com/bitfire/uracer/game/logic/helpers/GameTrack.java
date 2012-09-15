
package com.bitfire.uracer.game.logic.helpers;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.world.GameWorld;

/** Represents a view onto the logical portion of a game level track: it does sectorization and can perform queries against its
 * length.
 * 
 * @author bmanuel */
public final class GameTrack implements Disposable {

	// dbg render
	private final DebugRenderer dbg;
	private final boolean isDebugged;

	private final List<Vector2> route;
	private final List<Polygon> polys;
	private final TrackSector[] sectors;
	private final float totalLength;
	private final float oneOnTotalLength;

	Vector2 tmp = new Vector2();

	/** Represents a track sector, see Game Programming Gems pag. 416 */
	private static class TrackSector {
		public final Polygon poly;
		public final float length;
		public final float relativeTotal;
		public final Vector2 leading;
		public final Vector2 trailing;
		public final Vector2 nLeading;
		public final Vector2 nTrailing;

		public TrackSector (Polygon poly, float length, float relativeTotalLength, Vector2 leading, Vector2 trailing) {
			this.poly = poly;
			this.length = length;
			this.relativeTotal = relativeTotalLength;
			this.leading = leading;
			this.trailing = trailing;

			this.nLeading = new Vector2();
			this.nLeading.set(leading).sub(trailing).nor();

			this.nTrailing = new Vector2();
			this.nTrailing.set(trailing).sub(leading).nor();
		}
	}

	public GameTrack (GameWorld gameWorld) {
		this.route = gameWorld.getTrackRoute();
		this.polys = gameWorld.getTrackPolygons();
		this.sectors = new TrackSector[route.size()];

		totalLength = sectorize();
		oneOnTotalLength = 1f / totalLength;
		Gdx.app.log("RouteTracker", "total waypoint length = " + totalLength);

		if (Config.Debug.RenderTrackSectors) {
			dbg = new DebugRenderer(this, sectors, route);
			isDebugged = true;
		} else {
			dbg = null;
			isDebugged = false;
		}
	}

	/** Load and follows the supplied route waypoints, identifying sectors containing the two leading/trailing waypoints. The
	 * waypoint structure direction IS important and should follow the intended playing direction.
	 * 
	 * @return the total length of the track as computed by the sum of the lengths of a waypoint to the following one. */
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

			float len = from.dst(to);

			int p = findPolygon(from, to);
			if (p == -1) {
				throw new GdxRuntimeException("Cannot find a matching sectors for points (" + (i - 1) + "," + i + ")");
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
		if (isDebugged) {
			dbg.dispose();
		}
	}

	public void setDebugCar (Car car) {
		if (isDebugged) {
			dbg.setCar(car);
		}
	}

	public float getTotalLength () {
		return totalLength;
	}

	/** Returns the distance, in meters, of the specified car relative to the starting line of the current track */
	public float getTrackDistance (Car car) {
		Vector2 pt = car.getWorldPosMt();
		int carSector = findSector(pt);

		if (carSector != -1) {
			TrackSector s = sectors[carSector];
			float dist = MathUtils.clamp(distInSector(s, pt), 0, 1);
			float carlen = (s.relativeTotal + s.length * dist);
			return carlen;
		}

		return -1;
	}

	/** Returns a value in the [0,1] range, meaning the specified car is at the start (0) or at the end (1) of the lap. If the car
	 * is not on track, then a value lower than 0 is returned. */
	public float getTrackCompletion (Car car) {
		float ret = -1;
		float cardist = getTrackDistance(car);
		if (cardist > 0) {
			ret = (cardist * oneOnTotalLength);
		}

		return ret;
	}

	private Vector2 vlp = new Vector2();
	private Vector2 vtp = new Vector2();

	private float distInSector (TrackSector sector, Vector2 p) {
		float fdl, fdt;

		vlp.x = p.x - sector.leading.x;
		vlp.y = p.y - sector.leading.y;

		vtp.x = p.x - sector.trailing.x;
		vtp.y = p.y - sector.trailing.y;

		fdl = vlp.x * sector.nLeading.x + vlp.y * sector.nLeading.y;
		fdt = vtp.x * sector.nTrailing.x + vtp.y * sector.nTrailing.y;

		return fdl / (fdl + fdt);
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

	private static class DebugRenderer implements Disposable, GameRendererEvent.Listener {

		private final ShapeRenderer shape;
		private Car car;
		private boolean hasCar;
		private final TrackSector[] sectors;
		private final List<Vector2> route;
		private final GameTrack track;

		private final Vector2 tmp = new Vector2();

		public DebugRenderer (GameTrack track, TrackSector[] sectors, List<Vector2> route) {
			this.shape = new ShapeRenderer();
			this.sectors = sectors;
			this.route = route;
			this.car = null;
			this.track = track;
			hasCar = false;

			GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
		}

		@Override
		public void dispose () {
			GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
			shape.dispose();
		}

		@Override
		public void gameRendererEvent (Type type, Order order) {
			render();
		}

		private void render () {
			float alpha = 0.25f;
			float ialpha = 1f / 0.25f;
			int carSector = -1;

			if (hasCar) {
				carSector = track.findSector(car.getWorldPosMt());
			}

			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			shape.setProjectionMatrix(GameEvents.gameRenderer.mtxOrthographicMvpMt);

			tmp.set(route.get(0));
			shape.begin(ShapeType.Line);
			for (int i = 1; i <= route.size() - 1; i++) {
				Vector2 to = route.get(i);
				shape.setColor(1, 1, 1, alpha);
				shape.line(tmp.x, tmp.y, to.x, to.y);
				tmp.set(to);
			}
			shape.end();

			// draw dots
			shape.begin(ShapeType.FilledCircle);
			for (int i = 0; i < route.size(); i++) {
				Vector2 p = route.get(i);
				shape.setColor(0.8f, 1, 0.9f, alpha);
				shape.filledCircle(p.x, p.y, 0.5f, 100);
			}
			shape.end();

			// sectors
			shape.begin(ShapeType.Line);
			for (int i = 0; i < sectors.length; i++) {
				Polygon p = sectors[i].poly;

				if (i == carSector) {
					continue;
				}

				shape.setColor(1, 1, 1, alpha);
				drawSector(sectors[i]);
			}

			// car sector
			float carAlpha = (1 / alpha) * 0.6f;
			if (carSector > -1) {
				shape.setColor(1, 0, 0, alpha * carAlpha);
				drawSector(sectors[carSector]);
			}

			shape.end();

			// car sector's dots
			if (carSector > -1) {
				TrackSector s = sectors[carSector];

				shape.begin(ShapeType.FilledCircle);

				shape.setColor(1f, 0, 0f, alpha * carAlpha);
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

		public void setCar (Car car) {
			this.car = car;
			hasCar = (this.car != null);
		}
	}

}
