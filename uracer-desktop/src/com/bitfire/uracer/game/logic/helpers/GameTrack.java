
package com.bitfire.uracer.game.logic.helpers;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

/** Implements the track sectorizer and represents a view onto the logical portion of a track: it does sectorization and can be
 * queried against tracklength-based information, such as normalized position in track-coordinate space.
 * 
 * @author bmanuel */
public final class GameTrack {
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
		Gdx.app.log("GameTrack", "total length = " + totalLength);
	}

	private int findPolygon (Vector2 a, Vector2 b) {
		for (int i = 0; i < polys.size(); i++) {
			Polygon p = polys.get(i);
			// mitigate errors for small differences
			p.scale(0.1f);
			if (p.contains(a.x, a.y) && p.contains(b.x, b.y)) {
				p.scale(-0.1f); // restored
				return i;
			}

			p.scale(-0.1f); // restored
		}

		return -1;
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

			Gdx.app.log("GameTrack::sectorizer", from + " -> " + to + ", poly=" + p + ", len=" + len);

			accuLength += len;
		}

		return accuLength;
	}

	public float getTotalLength () {
		return totalLength;
	}

	public List<Vector2> getRoute () {
		return route;
	}

	public TrackSector[] getSectors () {
		return sectors;
	}

	/** Computes track position and orientation for the specified sector index offset, 1 is one sector ahead the from-editor
	 * starting point, -1 is the previous.
	 * 
	 * Asking for negative values is useful to permit the player some type "warm up"
	 * @param sectorIndexOffset
	 * @return TrackPosition */
	public TrackPosition generateTrackPosition (int sectorIndexOffset) {
		int s_index = sectorIndexOffset;

		if (s_index < 0) {
			s_index = route.size() - 1 + s_index + 1;
		}

		s_index = s_index % route.size();
		int e_index = (s_index + 1) % route.size();

		tmp.set(route.get(s_index)).sub(route.get(e_index));
		tmp.nor();

		return new TrackPosition(route.get(s_index), VMath.toRadians(tmp));
	}

	/** Returns the distance, in meters, of the specified car relative to the starting line of the current track. If the car is not
	 * on track then the value specified by the @param retDefault is returned. */
	public float getTrackDistance (Car car, float retDefault) {
		Vector2 pt = car.getWorldPosMt();
		int carSector = car.getTrackState().curr;
		if (carSector != -1) {
			TrackSector s = sectors[carSector];
			float dist = distanceInSector(s, pt);
			float carlen = (s.relativeTotal + s.length * dist);
			return AMath.fixup(carlen);
		}

		return AMath.fixup(retDefault);
	}

	/** Returns a value in the [0,1] range, meaning the specified car is at the start (0) or at the end (1) of the track. If the car
	 * is not on track then 0 is returned. */
	public float getTrackCompletion (Car car) {
		float ret = 0;
		float cardist = getTrackDistance(car, 0);
		if (cardist > 0) {
			ret = cardist * oneOnTotalLength;
		}

		return AMath.fixup(ret);
	}

	/** Returns a value in the [-1,1] range, meaning the specified car is following the path with a confidence value as expressed by
	 * the returned value.
	 * @param car
	 * @return The confidence value with which a car is following the current waypoint path. */
	public float getTrackRouteConfidence (Car car) {
		TrackState state = car.getTrackState();

		// car is on the expected path, now check for the correct heading
		if (state.onExpectedPath) {
			TrackSector s = sectors[state.curr];
			Vector2 heading = VMath.fromRadians(car.getWorldOrientRads());
			Vector2 dir = tmp.set(s.nLeading);

			// switch coordinate space and rotate it so that both the car and the track sector converge
			// to the same value when they are ~parallel and pointing to the same direction
			dir.scl(-1, 1);

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

	public void resetTrackState (Car car) {
		TrackState state = car.getTrackState();
		Vector2 pos = car.getWorldPosMt();
		state.curr = findSector(pos);
		state.next = state.curr + 1;
		if (state.next == sectors.length) state.next = 0;
		state.onExpectedPath = true;
		state.initialCompletion = getTrackCompletion(car);
	}

	public void updateTrackState (Car car) {
		TrackState state = car.getTrackState();
		state.updates++;

		Vector2 pos = car.getWorldPosMt();

		boolean inCurr = pointInSector(pos, state.curr);
		boolean inNext = pointInSector(pos, state.next);

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
			if (state.next == sectors.length) state.next = 0;
		}
	}

	private Vector2 vlp = new Vector2();
	private Vector2 vtp = new Vector2();

	public float distanceInSector (TrackSector sector, Vector2 p) {
		float fdl, fdt;

		vlp.x = p.x - sector.leading.x;
		vlp.y = p.y - sector.leading.y;

		vtp.x = p.x - sector.trailing.x;
		vtp.y = p.y - sector.trailing.y;

		fdl = vlp.x * sector.nLeading.x + vlp.y * sector.nLeading.y;
		fdt = vtp.x * sector.nTrailing.x + vtp.y * sector.nTrailing.y;

		return fdl / (fdl + fdt);
	}

	public float distanceInSector (int sectorIndex, Vector2 p) {
		TrackSector s = sectors[sectorIndex];
		return distanceInSector(s, p);
	}

	private boolean pointInSector (Vector2 point, int sector) {
		return sectors[sector].poly.contains(point.x, point.y);
	}

	private int findSector (Vector2 a) {
		for (int i = 0; i < sectors.length; i++) {
			TrackSector s = sectors[i];
			Polygon p = s.poly;
			if (p.contains(a.x, a.y)) {
				return i;
			}
		}

		return -1;
	}

	/** Represents a track sector, see Game Programming Gems 1, pag. 416 */
	public static class TrackSector {
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

	/** Represents position and orientation in a game track */
	public static class TrackPosition {
		public Vector2 position;
		public float orientation;

		public TrackPosition (Vector2 position, float orientation) {
			this.position = new Vector2(position);
			this.orientation = orientation;
		}
	}

	// FIXME move out of here
	/** Represents the current track state of a car */
	public static class TrackState {
		public int curr, next; // sectors
		public boolean onExpectedPath;
		public float initialCompletion;
		public int updates;

		// ghost only
		public boolean ghostArrived;
	}
}
