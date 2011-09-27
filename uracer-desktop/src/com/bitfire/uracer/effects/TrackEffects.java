package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.AMath;

public class TrackEffects
{
	private static int MaxSkidMarks = 2000;
	private static GameLogic logic;

	private static SpriteBatch driftsBatchFront;
	private static SpriteBatch driftsBatchRear;
	private static ArrayList<CarDrifts> driftMarks;
	private static int driftIndex;

	private static Car player;
	private static CarModel model;

	public static void init( GameLogic logic )
	{
		TrackEffects.logic = logic;

		driftIndex = 0;
		driftMarks = new ArrayList<CarDrifts>( MaxSkidMarks );

		player = logic.getGame().getLevel().getPlayer();
		model = player.getCarModel();

		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.add( new CarDrifts( player ) );
		}

		driftsBatchFront = new SpriteBatch( MaxSkidMarks, 10 );
		driftsBatchRear = new SpriteBatch( MaxSkidMarks, 10 );
	}

	public static void tick()
	{
		updateDriftMarks();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.get(i).tick();
		}
	}

	public static void render()
	{
		OrthographicCamera cam = Director.getCamera();

		// optimize drawing, group by texture

		driftsBatchFront.setProjectionMatrix( cam.projection );
		driftsBatchFront.setTransformMatrix( cam.view );
		driftsBatchRear.setProjectionMatrix( cam.projection );
		driftsBatchRear.setTransformMatrix( cam.view );

		// front drift marks
		driftsBatchFront.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.get( i ).renderFront( driftsBatchFront );
		}
		driftsBatchFront.end();

		// rear drift marks
		driftsBatchRear.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.get( i ).renderRear( driftsBatchRear );
		}
		driftsBatchRear.end();
	}

	public static void reset()
	{
		driftIndex = 0;
	}

	public static void dispose()
	{
		driftMarks.clear();
	}

	/**
	 * Skid marks
	 *
	 */

	private static void updateDriftMarks()
	{
		Vector2 pos = player.state().position;
		float angle = player.state().orientation;

		// lateral forces are in the range [-max_grip, max_grip]
		float flatf = Math.abs( player.getSimulator().lateralForceFront.y );
		flatf = AMath.clamp( flatf / model.max_grip, 0, 1f );

		float flatr = Math.abs( player.getSimulator().lateralForceRear.y );
		flatr = AMath.clamp( flatr / model.max_grip, 0, 1f );

		// add front drift marks?
		CarDrifts drift = driftMarks.get( driftIndex++ );
		if( driftIndex == MaxSkidMarks ) driftIndex = 0;

		float from = 0f;
		float div = 1f - from;
		float af = (flatf - from) / div;
		float ar = (flatr - from) / div;
		drift.setAlpha( af, ar );
		drift.setPosition( pos );
		drift.setOrientation( angle );
		drift.maxLife = MathUtils.random() * 10f + 21f;
		drift.life = drift.maxLife;
	}
}
