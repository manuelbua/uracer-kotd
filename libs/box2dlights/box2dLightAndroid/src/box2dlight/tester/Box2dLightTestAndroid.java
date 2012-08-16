package box2dlight.tester;

import testCase.Box2dLightTest;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class Box2dLightTestAndroid extends AndroidApplication {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();		
		config.useCompass = false;
		config.useAccelerometer = false;	
		config.useGL20 = true;
		config.depth = 0;
		initialize(new Box2dLightTest(), config);
	}
}