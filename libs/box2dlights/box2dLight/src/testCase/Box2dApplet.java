package testCase;

import com.badlogic.gdx.backends.lwjgl.LwjglApplet;

/** THIS IS NOT INDEPENDENT AND RUNNABLE */
@SuppressWarnings("serial")
public class Box2dApplet extends LwjglApplet {
	public Box2dApplet() {
		super(new Box2dLightTest(), true);
	}
}
