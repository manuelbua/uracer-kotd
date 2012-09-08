
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
	Vector2 tmp = new Vector2();
	private int nearestPoint = -1;

	public RouteTracker (List<Vector2> route, GameWorldRenderer gameWorldRenderer) {
		this.route = route;
		this.gameWorldRenderer = gameWorldRenderer;

		tmp.set(route.get(0));
		for (int i = 1; i <= route.size() - 1; i++) {
			Vector2 to = route.get(i);

			Gdx.app.log("RouteTracker", tmp + " -> " + to);

			tmp.set(to);
		}

		GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
	}

	@Override
	public void dispose () {
		GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);
	}

	public void setCar (Car car) {
		this.car = car;
	}

	public void update () {
		if (car == null) {
			return;
		}

		Vector2 playerPos = car.getWorldPosMt();

		nearestPoint = 0;
		float d = playerPos.dst2(route.get(0));
		for (int i = 1; i <= route.size() - 1; i++) {
			float tmp = playerPos.dst2(route.get(i));
			if (tmp < d) {
				nearestPoint = i;
				d = tmp;
			}
		}
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
			if (i == nearestPoint) {
				shape.setColor(1f, 0.2f, 0.2f, 0.85f);
				shape.filledCircle(p.x, p.y, 0.8f, 100);
			} else {
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

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
}
