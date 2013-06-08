
package com.bitfire.uracer.events;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

public class GameRendererEvent extends Event<GameRendererEvent.Type, GameRendererEvent.Order, GameRendererEvent.Listener> {
	/** defines the type of render queue */
	public enum Type {
		OnSubframeInterpolate, BatchBeforeCars, BatchAfterCars, BatchBeforePostProcessing, BatchAfterPostProcessing, BatchDebug, Debug;
	}

	/** defines the position in the render queue specified by the Type parameter */
	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public SpriteBatch batch;
	public Matrix4 mtxOrthographicMvpMt;
	public PerspectiveCamera camPersp;
	public OrthographicCamera camOrtho;
	public float timeAliasingFactor;

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public GameRendererEvent () {
		super(Type.class, Order.class);
	}
}
