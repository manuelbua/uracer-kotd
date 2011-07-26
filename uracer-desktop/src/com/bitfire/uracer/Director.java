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
		camWorld = new OrthographicCamera( Physics.px2mt( widthPx ), Physics.px2mt( heightPx ) );
		camWorld.position.set( Physics.px2mt( positionPx.x ), Physics.px2mt( positionPx.y ), 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( positionPx.x, positionPx.y, 0 );
	}

	public static void createFromMeters(float widthMt, float heightMt, Vector2 positionMt)
	{
		camWorld = new OrthographicCamera( widthMt, heightMt );
		camWorld.position.set( positionMt.x, positionMt.y, 0 );

		camScreen = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		camScreen.position.set( Physics.mt2px(positionMt.x), Physics.mt2px(positionMt.y), 0 );
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

	public static void setPositionPx(Vector2 pos)
	{
		camWorld.position.set( Physics.px2mt( pos.x ), Physics.px2mt( pos.y ), 0 );
		camScreen.position.set( pos.x, pos.y, 0 );
	}

	public static void setPositionMt(Vector2 pos)
	{
		camWorld.position.set( pos.x, pos.y, 0 );
		camScreen.position.set( Physics.mt2px(pos.x), Physics.mt2px(pos.y), 0 );
	}
}
