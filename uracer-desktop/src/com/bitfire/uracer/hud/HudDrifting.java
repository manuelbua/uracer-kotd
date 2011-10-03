package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.utils.Convert;

public class HudDrifting
{
	private GameLogic logic;
	private Car player;
	private CarModel model;
	private int carWidthPx, carLengthPx;

	private HudLabel labelRealtime, labelResult;
	private DriftInfo drift;

	public HudDrifting( GameLogic logic )
	{
		this.logic = logic;
		this.player = logic.getGame().getLevel().getPlayer();
		this.model = player.getCarModel();
		carWidthPx = (int)Convert.mt2px( model.width );
		carLengthPx = (int)Convert.mt2px( model.length );

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99", 0.5f );
		labelRealtime.setAlpha( 0 );

		labelResult = new HudLabel( Art.fontCurseR, "99.99", 0.5f );
		labelResult.setAlpha( 0 );
	}

	public void reset()
	{
		labelRealtime.setAlpha( 0 );
		labelResult.setAlpha( 0 );
	}

	public void tick()
	{
		drift = DriftInfo.get();
	}

	private Vector2 tmpv = new Vector2();
	public void render( SpriteBatch batch )
	{
		// update from subframe-interpolated player position
		Vector2 pos = tmpv.set( Director.screenPosForPx( player.state().position ) );
		Vector2 heading = player.getSimulator().heading;

//		labelResult.setPosition(
//				pos.x - heading.x * (carWidthPx + labelResult.halfBoundsWidth),
//				pos.y - heading.y * (carLengthPx + labelResult.halfBoundsHeight)
//			);

		labelRealtime.setPosition(
				// offset by heading.mul(distance factor)
				pos.x - heading.x * (carWidthPx + labelRealtime.halfBoundsWidth),
				pos.y - heading.y * (carLengthPx + labelRealtime.halfBoundsHeight)
			);

		//
		// draw earned seconds
		//
		labelRealtime.setString( "+" + String.format( "%.02f", drift.driftSeconds ) );
		labelRealtime.render( batch );

		//
		// draw lost seconds
		//
		labelResult.render( batch );
	}

	public void onBeginDrift()
	{
		labelRealtime.fadeIn( 100 );
	}

	private Vector2 heading = new Vector2();
	public void onEndDrift()
	{
		Vector2 pos = tmpv.set( Director.screenPosForPx( player.state().position ) );
		heading.set(player.getSimulator().heading);

		labelRealtime.fadeOut( 100 );

		labelResult.setPosition(
				pos.x - heading.x * (carWidthPx + labelResult.halfBoundsWidth),
				pos.y - heading.y * (carLengthPx + labelResult.halfBoundsHeight)
			);

		// premature end drift event due to collision?
		if( drift.hasCollided )
		{
			labelResult.slide( heading, 10, 50 );
			labelResult.setString( "-" + String.format( "%.02f", drift.driftSeconds ) );
			labelResult.setFont( Art.fontCurseRbig );
//			labelResult.fadeInFor( 200, 400 );
		}
		else
		{
			labelResult.slide( heading.mul( -1 ), 10, 50 );
			labelResult.setString( "+" + String.format( "%.02f", drift.driftSeconds ) );
			labelResult.setFont( Art.fontCurseGbig );
//			labelResult.fadeInFor( 200, 400 );

			if( drift.driftSeconds >= 1 && drift.driftSeconds < 1.5f )
			{
				Messager.enqueue( "NICE ONE!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f, MessageType.Good, MessagePosition.Middle, MessageSize.Big );
			}
			else if( drift.driftSeconds >= 1.5f && drift.driftSeconds < 2f )
			{
				Messager.enqueue( "FANTASTIC!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f, MessageType.Good, MessagePosition.Middle, MessageSize.Big );
			}
			else if( drift.driftSeconds >= 2f )
			{
				Messager.enqueue( "UNREAL!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f, MessageType.Good, MessagePosition.Middle, MessageSize.Big );
			}
		}
	}
}
