package com.bitfire.uracer;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;

public class URacerAndroid extends AndroidApplication
{
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		initialize( new URacer(), true );
	}
}