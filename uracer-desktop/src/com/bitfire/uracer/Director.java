package com.bitfire.uracer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.testtilemap.ScalingStrategy;

public class Director
{
	private static OrthographicCamera camera;
	public static Vector2 worldSizePx, worldSizeMt;
	private static Vector2 screenPosFor;
	private static Matrix4 mvpMt, mvpPx;
	private static ScalingStrategy strategy;

	public static void init(ScalingStrategy strategy_)
	{
		worldSizePx = new Vector2();
		worldSizeMt = new Vector2();
		screenPosFor = new Vector2();
		mvpMt = new Matrix4();
		mvpPx = new Matrix4();
		strategy = strategy_;
	}

	public static void createFromPixels( ScalingStrategy strategy, int widthPx, int heightPx, Vector2 positionPx, Vector2 worldSizePx )
	{
		init(strategy);

		camera = new OrthographicCamera( widthPx, heightPx );

		Director.worldSizePx = worldSizePx;
		Director.worldSizeMt = Physics.px2mt( worldSizePx );

		setPositionPx( positionPx, true );
		update();
	}

	public static void createFromMeters( ScalingStrategy strategy, float widthMt, float heightMt, Vector2 positionMt, Vector2 worldSizeMt )
	{
		init(strategy);

		camera = new OrthographicCamera( Physics.mt2px(widthMt), Physics.mt2px(heightMt) );

		Director.worldSizeMt = worldSizeMt;
		Director.worldSizePx = Physics.mt2px( worldSizePx );

		setPositionMt( positionMt, true );
		update();
	}

	public static OrthographicCamera getCamera()
	{
		return camera;
	}

	public static void update()
	{
		camera.update();

		mvpPx.set( camera.combined );
		mvpMt.set( mvpPx );

		// scale px to meters
		// TODO: figure out why mt2px instead of px2mt..
		mvpMt.val[Matrix4.M00] = Physics.mt2px(mvpPx.val[Matrix4.M00]);
		mvpMt.val[Matrix4.M01] = Physics.mt2px(mvpPx.val[Matrix4.M01]);
		mvpMt.val[Matrix4.M10] = Physics.mt2px(mvpPx.val[Matrix4.M10]);
		mvpMt.val[Matrix4.M11] = Physics.mt2px(mvpPx.val[Matrix4.M11]);
	}

	public static void setPositionPx( Vector2 pos, boolean flipY )
	{
		if(flipY)
		{
			camera.position.set( pos.x, worldSizePx.y - pos.y, 0 );
		}
		else
		{
			camera.position.set( pos.x, pos.y, 0 );
		}
	}

	public static void setPositionMt( Vector2 pos, boolean flipY )
	{
		if(flipY)
		{
			camera.position.set( Physics.mt2px( pos.x ), worldSizePx.y - Physics.mt2px( pos.y ), 0 );
		}
		else
		{
			camera.position.set( Physics.mt2px( pos.x ), Physics.mt2px( pos.y ), 0 );

		}
	}

	public static Vector3 pos()
	{
		return camera.position;
	}

	public static Vector2 screenPosFor( Body body )
	{
//		System.out.println(body.getPosition());
		screenPosFor.x = Physics.mt2px(body.getPosition().x) - Director.getCamera().position.x + camera.viewportWidth/2f;
//		screenPosFor.y = Physics.mt2px(worldSizeMt.y-body.getPosition().y) - (worldSizePx.y-Director.getCamPixels().position.y) + camPixels.viewportHeight/2f;
//		screenPosFor.y = Physics.mt2px(body.getPosition().y) - Director.getCamPixels().position.y + camPixels.viewportHeight/2f;
		screenPosFor.y = Director.getCamera().position.y - Physics.mt2px(body.getPosition().y) + camera.viewportHeight/2f;

//		if(strategy!=null) screenPosFor.mul( 1f/strategy.tileMapZoomFactorAtRef );

		return screenPosFor;
	}

	public static Matrix4 getMatViewProjPx()
	{
		return mvpPx;
	}

	public static Matrix4 getMatViewProjMt()
	{
		return mvpMt;
	}
}
