
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
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarTrackState;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

/** Implements the track sectorizer and represents a view onto the logical portion of a track: it does sectorization and can be
 * queried against tracklength-based information, such as normalized position in track-coordinate space.
 * 
 * @author bmanuel */
public final class GameTrack implements Disposable {

	private final DebugRenderer dbg;

	private final List<Vector2> route;
	private final List<Polygon> polys;
	private final TrackSector[] sectors;
	private final float totalLength;
	private final float oneOnTotalLength;
	private Vector2 tmp = new Vector2();

	public GameTrack (final List<Vector2> route, final List<Polygon> trackPoly) {
		this.route = route;
		this.polys = trackPoly;
		this.sectors = new TrackSector[route.size()];

		totalLength = sectorize();
		oneOnTotalLength = 1f / totalLength;
		Gdx.app.log("RouteTracker", "total waypoint length = " + totalLength);

		dbg = new DebugRenderer(sectors, route);
	}

	public void showDebug (boolean show) {
		if (show) {
			dbg.attach();
		} else {
			dbg.detach();
		}
	}

	/** Load and follows the supplied route waypoints, identifying sectors containing the two leading/trailing waypoints. The
	 * waypoint structure walking direction at creation time IS important and should follow the intended playing direction.
	 * 
	 * @return the total length of the track as computed by the sum of the lengths between a waypoint and the next. */
	private float sectorize () {
		float accuLength = 0;
		for (int i = 0; i < route.size(); i++) {
			Vector2 from = route.get(i);
			Vector2 to;

			if (i == route.size() - 1) {
				// chain last to first
				to = route.get(0);
			} else {
				to = route.get(i + 1);
			}

			float len = from.dst(to);

			int p = findPolygon(from, to);
			if (p == -1) {
				throw new GdxRuntimeException("Cannot find a matching sectors for (" + (i - 1) + "," + i + ")");
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
		dbg.dispose();
	}

	public void setDebugCar (PlayerCar car) {
		dbg.setCar(car);
	}

	public float getTotalLength () {
		return totalLength;
	}

	public void reset () {
	}

	/** Returns the distance, in meters, of the specified car relative to the starting line of the current track. If it fails
	 * determining the value, then the value specified by the @param retDefault is returned. */
	public float getTrackDistance (Car car, float retDefault) {
		Vector2 pt = car.getWorldPosMt();

		// if at starting position then returns 0
		if (AMath.equals(pt.x, route.get(0).x) && AMath.equals(pt.y, route.get(0).y)) {
			return 0;
		}

		int carSector = car.getTrackState().curr;

		if (carSector != -1) {
			TrackSector s = sectors[carSector];
			float dist = MathUtils.clamp(distInSector(s, pt), 0, 1);
			float carlen = (s.relativeTotal + s.length * dist);
			return AMath.fixup(carlen);
		}

		return AMath.fixup(retDefault);
	}

	/** Returns a value in the [0,1] range, meaning the specified car is at the start (0) or at the end (1) of the lap. If the car
	 * is not on track, then the value specified by the @param retDefault is returned. */
	public float getTrackCompletion (Car car, float retDefault) {
		float ret = retDefault;
		float cardist = getTrackDistance(car, 0);
		if (cardist > 0) {
			ret = (cardist * oneOnTotalLength);
		}

		return AMath.fixup(ret);
	}

	/** Returns a value in the [-1,1] range, meaning the specified car is following the path with a confidence value as expressed by
	 * the returned value.
	 * @param car
	 * @return The confidence value with which a car is following the current waypoint path. */
	public float getTrackRouteConfidence (Car car) {

		CarTrackState state = car.getTrackState();

		// car is on the expected path, now check for the correct heading
		if (state.onExpectedPath) {
			TrackSector s = sectors[state.curr];
			Vector2 heading = VMath.fromDegrees(car.state().orientation);
			Vector2 dir = tmp.set(s.nLeading);

			// switch coordinate space and rotate it so that both the car and the track sector converge
			// to the same value when they are ~parallel and pointing to the same direction
			dir.mul(-1, 1);

			float dot = dir.dot(heading);

			// @off
//			Gdx.app.log("GameTrack", "dir=" + NumberString.formatSmall(dir.x) + "," + NumberString.formatSmall(dir.y)
//				+ ", heading=" + NumberString.formatSmall(heading.x) + "," + NumberString.formatSmall(heading.x) + ", dot="
//				+ NumberString.formatSmall(dot));
			// @on

			return dot;
		}

		return -1;
	}

	public void updateTrackStates (Car car) {
		CarTrackState state = car.getTrackState();
		Vector2 pos = car.getWorldPosMt();

		boolean inCurr = lookupSector(pos, state.curr);
		boolean inNext = lookupSector(pos, state.next);

		// start position/polygon.contains mismatch fix
		if (state.curr == 0 && getTrackCompletion(car, 0) == 0) {
			inCurr = true;
		}

		state.onExpectedPath = true;

		if (!inCurr && !inNext) {
			state.onExpectedPath = false;
			return;
		}

		if (inNext) {
			// switched

			state.onExpectedPath = true;

			state.curr = state.next;
			state.next = state.curr + 1;

			if (state.next == sectors.length) {
				state.next = 0;
			}
		}
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

	protected float distInSector (int sectorIndex, Vector2 p) {
		TrackSector s = sectors[sectorIndex];
		return distInSector(s, p);
	}

	private boolean lookupSector (Vector2 position, int sector) {
		return sectors[sector].poly.contains(position.x, position.y);
	}

	// search all sectors for the given point
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

	// search one sector ahead of the given sector for the given point
// private int findSector (Vector2 position, int fromSector) {
// int from = MathUtils.clamp(fromSector, 0, sectors.length - 1);
// if (from == sectors.length - 1) {
// from = 0;
// }
//
// int to = from + 1;
//
// for (int i = from; i <= to; i++) {
// TrackSector s = sectors[i];
// Polygon p = s.poly;
// if (p.contains(position.x, position.y)) {
// return i;
// }
// }
//
// return -1;
// }

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
	// add naive debug output
	//

	// TODO
	// export this class style as DebugRenderable or something like that
	private class DebugRenderer implements Disposable, GameRendererEvent.Listener {

		private final ShapeRenderer shape;
		private PlayerCar car;
		private boolean hasCar;
		private final TrackSector[] sectors;
		private final List<Vector2> route;

		// private final Vector2 tmp = new Vector2();
		private boolean attached = false;

		public DebugRenderer (TrackSector[] sectors, List<Vector2> route) {
			this.shape = new ShapeRenderer();
			this.sectors = sectors;
			this.route = route;
			this.car = null;
			hasCar = false;
		}

		public void attach () {
			if (!attached) {
				GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
				attached = true;
			}
		}

		public void detach () {
			if (attached) {
				GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
				attached = false;
			}
		}

		@Override
		public void dispose () {
			detach();
			shape.dispose();
		}

		@Override
		public void gameRendererEvent (Type type, Order order) {
			render();
		}

		private void render () {
			float alpha = 0.25f;
			float carAlpha = alpha * 3;
			float sectorCenterFactor = 0;

			int carSector = -1;

			if (hasCar) {
				carSector = car.getTrackState().curr;

				if (carSector > -1) {
					float d = MathUtils.clamp(distInSector(carSector, car.getWorldPosMt()), 0, 1);

					if (d < 0.5f) {
						sectorCenterFactor = d / 0.5f;
					} else {
						d -= 0.5f;
						d *= 2;
						sectorCenterFactor = MathUtils.clamp(1 - d, 0, 1);
					}
				}
			}

			// precompute alpha from center factor
			float scol = 1 - sectorCenterFactor;
			float sa = alpha + (carAlpha - alpha) * sectorCenterFactor;

			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			shape.setProjectionMatrix(GameEvents.gameRenderer.mtxOrthographicMvpMt);

			// draw waypoint segments
			shape.begin(ShapeType.Line);
			for (int i = 0; i < sectors.length; i++) {
				TrackSector s = sectors[i];

				if (carSector == i) {
					shape.setColor(1, 1, scol, sa);
				} else {
					shape.setColor(1, 1, 1, alpha);
				}

				shape.line(s.leading.x, s.leading.y, s.trailing.x, s.trailing.y);
			}
			shape.end();

			// draw dots
			shape.begin(ShapeType.Filled);
			for (int i = 0; i < route.size(); i++) {
				Vector2 p = route.get(i);
				shape.setColor(1, 1, 1, alpha);
				shape.circle(p.x, p.y, 0.5f, 100);
			}
			shape.end();

			// sectors
			shape.begin(ShapeType.Line);
			for (int i = 0; i < sectors.length; i++) {
				if (i == carSector) {
					continue;
				}

				shape.setColor(1, 1, 1, alpha);
				drawSector(sectors[i]);
			}

			// car sector
			if (carSector > -1) {
				shape.setColor(1, 1, scol, sa);
				drawSector(sectors[carSector]);
			}

			shape.end();

			// car sector's dots
			if (carSector > -1) {
				TrackSector s = sectors[carSector];

				shape.begin(ShapeType.Filled);

				shape.setColor(1, 1, scol, sa - alpha);
				shape.circle(s.leading.x, s.leading.y, 0.5f, 100);
				shape.circle(s.trailing.x, s.trailing.y, 0.5f, 100);

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

		public void setCar (PlayerCar car) {
			this.car = car;
			hasCar = (this.car != null);
		}
	}

	/** Represents a track sector, see Game Programming Gems pag. 416 */
	private static class TrackSector {
		public final Polygon poly;
		public final float length;
		public final float relativeTotal;

		// do not allocate new vectors, just reference the original ones and
		// treat them as read-only data
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
}
