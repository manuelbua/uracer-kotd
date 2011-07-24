package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

public class Director
{
	// camWorld (mt)
	// camScreen (px)
	private static OrthographicCamera camScreen, camWorld;

	public static void createFromPixels(int widthPx, int heightPx, Vector2 positionPx)
	{
		camWorld = new OrthographicCamera( Physics.s2w( widthPx ), Physics.s2w( heightPx ) );
		camWorld.position.set( Physics.s2w( positionPx.x ), Physics.s2w( positionPx.y ), 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( positionPx.x, positionPx.y, 0 );
	}

	public static void createFromMeters(float widthMt, float heightMt, Vector2 positionMt)
	{
		camWorld = new OrthographicCamera( widthMt, heightMt );
		camWorld.position.set( positionMt.x, positionMt.y, 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( Physics.w2s(positionMt.x), Physics.w2s(positionMt.y), 0 );

	}

	public static OrthographicCamera getScreenCam()
	{
		return camScreen;
	}

	public static OrthographicCamera getWorldCam()
	{
		return camWorld;
	}

	public static void update()
	{
		camScreen.update();
		camWorld.update();
	}
}
