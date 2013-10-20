
package com.bitfire.uracer.game.debug;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackSector;
import com.bitfire.uracer.game.player.PlayerCar;

public class GameTrackDebugRenderer extends DebugRenderable {

	private final ShapeRenderer shape;
	private Car car;
	private boolean hasCar;
	private final TrackSector[] sectors;
	private final List<Vector2> route;
	private final GameTrack gameTrack;

	public GameTrackDebugRenderer (RenderFlags flag, GameTrack gameTrack) {
		super(flag);

		this.gameTrack = gameTrack;
		this.sectors = gameTrack.getSectors();
		this.route = gameTrack.getRoute();
		this.shape = new ShapeRenderer();
		this.car = null;
		hasCar = false;
	}

	@Override
	public void dispose () {
		shape.dispose();
	}

	@Override
	public void tick () {
	}

	@Override
	public void player (PlayerCar player) {
		car = player;
		hasCar = (car != null);
	}

	private void drawSector (TrackSector sector) {
		Polygon p = sector.poly;
		float[] vertices = p.getTransformedVertices();
		shape.line(vertices[0], vertices[1], vertices[2], vertices[3]);
		shape.line(vertices[2], vertices[3], vertices[4], vertices[5]);
		shape.line(vertices[4], vertices[5], vertices[6], vertices[7]);
		shape.line(vertices[6], vertices[7], vertices[0], vertices[1]);
	}

	@Override
	public void render () {
		float alpha = 0.25f;
		float carAlpha = alpha * 3;
		float sectorCenterFactor = 0;

		int carSector = -1;

		if (hasCar) {
			carSector = car.getTrackState().curr;

			if (carSector > -1) {
				float d = MathUtils.clamp(gameTrack.distanceInSector(carSector, car.getWorldPosMt()), 0, 1);

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
}
