package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Director
{
	private static OrthographicCamera camPixels, camMeters;
	public static Vector2 worldSizePx, worldSizeMt;
	private static Vector2 screenPosFor;

	public static void init()
	{
		worldSizePx = new Vector2();
		worldSizeMt = new Vector2();
		screenPosFor = new Vector2();
	}

	public static void createFromPixels( int widthPx, int heightPx, Vector2 positionPx, Vector2 worldSizePx )
	{
		init();

		camMeters = new OrthographicCamera( Physics.px2mt( widthPx ), Physics.px2mt( heightPx ) );
		camPixels = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		Director.worldSizePx = worldSizePx;
		Director.worldSizeMt = Physics.px2mt( worldSizePx );

		setPositionPx( positionPx, true );
	}

	public static void createFromMeters( float widthMt, float heightMt, Vector2 positionMt, Vector2 worldSizeMt )
	{
		init();

		camMeters = new OrthographicCamera( widthMt, heightMt );
		camPixels = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		Director.worldSizeMt = worldSizeMt;
		Director.worldSizePx = Physics.mt2px( worldSizePx );

		setPositionMt( positionMt, true );
	}

	public static OrthographicCamera getCamPixels()
	{
		return camPixels;
	}

	public static OrthographicCamera getCamMeters()
	{
		return camMeters;
	}

	public static void update()
	{
		camPixels.update();
		camMeters.update();
	}

	public static void setPositionPx( Vector2 pos, boolean flipY )
	{
		if(flipY)
		{
			camMeters.position.set( Physics.px2mt( pos.x ), worldSizeMt.y - Physics.px2mt( pos.y ), 0 );
			camPixels.position.set( pos.x, worldSizePx.y - pos.y, 0 );
		}
		else
		{
			camMeters.position.set( Physics.px2mt( pos.x ), Physics.px2mt( pos.y ), 0 );
			camPixels.position.set( pos.x, pos.y, 0 );
		}
	}

	public static void setPositionMt( Vector2 pos, boolean flipY )
	{
		if(flipY)
		{
			camMeters.position.set( pos.x, worldSizeMt.y - pos.y, 0 );
			camPixels.position.set( Physics.mt2px( pos.x ), worldSizePx.y - Physics.mt2px( pos.y ), 0 );
		}
		else
		{
			camMeters.position.set( pos.x, pos.y, 0 );
			camPixels.position.set( Physics.mt2px( pos.x ), Physics.mt2px( pos.y ), 0 );

		}
	}

	public static Vector2 screenPosFor( Body body )
	{
//		System.out.println(body.getPosition());
		screenPosFor.x = Physics.mt2px(body.getPosition().x) - Director.getCamPixels().position.x + camPixels.viewportWidth/2f;
//		screenPosFor.y = Physics.mt2px(worldSizeMt.y-body.getPosition().y) - (worldSizePx.y-Director.getCamPixels().position.y) + camPixels.viewportHeight/2f;
//		screenPosFor.y = Physics.mt2px(body.getPosition().y) - Director.getCamPixels().position.y + camPixels.viewportHeight/2f;
		screenPosFor.y = Director.getCamPixels().position.y - Physics.mt2px(body.getPosition().y) + camPixels.viewportHeight/2f;

		return screenPosFor;
	}
}
