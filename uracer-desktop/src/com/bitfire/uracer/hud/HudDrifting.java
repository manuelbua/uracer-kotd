package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public class HudDrifting
{
	private Game game;
	private Car playerCar;
	private CarModel model;
	private int carWidthPx, carLengthPx;

	public HudLabel labelRealtime, labelResult;
	private DriftInfo drift;
	private Vector2 heading = new Vector2();

	public HudDrifting( Game game )
	{
		this.game = game;
		this.playerCar = game.getLevel().getPlayer().car;
		this.model = playerCar.getCarModel();
		carWidthPx = (int)Convert.mt2px( model.width );
		carLengthPx = (int)Convert.mt2px( model.length );

		drift = DriftInfo.get();

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99", 0.5f );
		labelRealtime.setAlpha( 0 );

		labelResult = new HudLabel( Art.fontCurseR, "99.99", 0.85f );
		labelResult.setAlpha( 0 );
	}

	public void reset()
	{
		labelRealtime.setAlpha( 0 );
		labelResult.setAlpha( 0 );
	}

	public void tick()
	{
		heading.set(playerCar.getSimulator().heading);
	}

	private Vector2 tmpv = new Vector2();
	private float lastDistance = 0f;
	public void render( SpriteBatch batch )
	{
		// update from subframe-interpolated player position
		Vector2 pos = tmpv.set( Director.screenPosForPx( playerCar.state().position ) );


		float secRatio = 1f;
		float distance = 0f;
		if( drift.isDrifting )
		{
//			secRatio = AMath.clamp( (System.currentTimeMillis() - drift.driftStartTime) / 2000f, 0, 1);
//			labelRealtime.setAlpha( secRatio );
//			distance = (1f-secRatio) * 50f;
//			lastDistance = distance;
			lastDistance = 0;
		}

		labelRealtime.setPosition(
			// offset by heading.mul(distance factor)
			pos.x - heading.x * (carWidthPx + labelRealtime.halfBoundsWidth + lastDistance),
			pos.y - heading.y * (carLengthPx + labelRealtime.halfBoundsHeight + lastDistance)
		);

		//
		// draw earned seconds
		//
		labelRealtime.setString( "+" + NumberString.format(drift.driftSeconds) );
		labelRealtime.render( batch );

		//
		// draw lost seconds
		//
		labelResult.render( batch );
	}

	public void onBeginDrift()
	{
		labelRealtime.fadeIn( 300 );
	}

	public void onEndDrift()
	{
		Vector2 pos = tmpv.set( Director.screenPosForPx( playerCar.state().position ) );

		labelRealtime.fadeOut( 300 );

		labelResult.setPosition(
				pos.x - heading.x * (carWidthPx + labelResult.halfBoundsWidth),
				pos.y - heading.y * (carLengthPx + labelResult.halfBoundsHeight)
			);

		// premature end drift event due to collision?
		if( drift.hasCollided )
		{
			labelResult.setString( "-" + NumberString.format(drift.driftSeconds) );
			labelResult.setFont( Art.fontCurseRbig );
		}
		else
		{
			labelResult.setString( "+" + NumberString.format(drift.driftSeconds) );
			labelResult.setFont( Art.fontCurseGbig );

			if( drift.driftSeconds >= 1 && drift.driftSeconds < 1.5f )
			{
				Messager.enqueue( "NICE ONE!\n+" + NumberString.format(drift.driftSeconds) + "  seconds!", 1f, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			}
			else if( drift.driftSeconds >= 1.5f && drift.driftSeconds < 2f )
			{
				Messager.enqueue( "FANTASTIC!\n+" + NumberString.format(drift.driftSeconds) + "  seconds!", 1f, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			}
			else if( drift.driftSeconds >= 2f )
			{
				Messager.enqueue( "UNREAL!\n+" + NumberString.format(drift.driftSeconds) + "  seconds!", 1f, MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			}
		}

		labelResult.slide();
	}
}
