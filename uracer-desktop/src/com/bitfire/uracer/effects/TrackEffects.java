package com.bitfire.uracer.effects;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

		driftsBatchFront = new SpriteBatch( MaxSkidMarks / 2 + 5, 5 );
		driftsBatchRear = new SpriteBatch( MaxSkidMarks / 2 + 5, 5 );

		tmp = new Vector2();
		last = new Vector2();
	}

	public static void tick()
	{
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			driftMarks.get( i ).tick();
		}
	}

	public static void render()
	{
		updateDriftMarks();

		OrthographicCamera cam = Director.getCamera();

		// optimize drawing, group by texture

		driftsBatchFront.setProjectionMatrix( cam.projection );
		driftsBatchFront.setTransformMatrix( cam.view );
		driftsBatchRear.setProjectionMatrix( cam.projection );
		driftsBatchRear.setTransformMatrix( cam.view );

		CarDrifts d;

		// front drift marks
		driftsBatchFront.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = driftMarks.get( i );
			if( d.life > 0 )
			{
				d.updateRatio();
				d.renderFront( driftsBatchFront );
			}
		}
		driftsBatchFront.end();

		// rear drift marks
		driftsBatchRear.begin();
		for( int i = 0; i < MaxSkidMarks; i++ )
		{
			d = driftMarks.get( i );
			if( d.life > 0 ) d.renderRear( driftsBatchRear );
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

	private static Vector2 tmp;
	private static Vector2 last;

	private static void updateDriftMarks()
	{
		if( player.getCarDescriptor().velocity_wc.len2() < 1 )
		{
			return;
		}

		tmp.set( player.state().position );
		float angle = player.state().orientation;

		if( (int)tmp.x == (int)last.x && (int)tmp.y == (int)last.y )
		{
			return;
		}

		// tmp.set( Convert.mt2px(player.getBody().getPosition()) );
		// float angle = player.getBody().getAngle() *
		// MathUtils.radiansToDegrees;

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
		if( af > 0 || ar > 0 )
		{
			drift.setAlpha( af, ar );
			drift.setPosition( tmp );
			drift.setOrientation( angle );
			drift.maxLife = 5f;
			drift.life = drift.maxLife;

			last.set( tmp );
		} else
		{
			drift.life = 0;
		}
	}
}
