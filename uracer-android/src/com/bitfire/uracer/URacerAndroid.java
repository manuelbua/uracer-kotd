package com.bitfire.uracer;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.bitfire.uracer.URacer.URacerFinalizer;

public class URacerAndroid extends AndroidApplication
{
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGL20 = true;

		URacer uracer = new URacer();
		initialize( uracer, config );

		AndroidFinalizer finalizer = new AndroidFinalizer( this.audio );
		uracer.setFinalizer( finalizer );
	}

	private class AndroidFinalizer implements URacerFinalizer
	{
		private AndroidAudio audio = null;

		public AndroidFinalizer(AndroidAudio audio)
		{
			this.audio = audio;
		}

		@Override
		public void dispose()
		{
			if(this.audio != null)
			{
				this.audio.dispose();
				this.audio = null;
			}
		}
	}
}