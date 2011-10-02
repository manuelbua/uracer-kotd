package com.bitfire.uracer.hud;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenGroup;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
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
import com.bitfire.uracer.tweenables.TweenHudLabel;
import com.bitfire.uracer.utils.Convert;

public class HudDrifting
{
	private GameLogic logic;
	private Car player;
	private CarModel model;
	private int carWidthPx, carLengthPx;

	private HudLabel labelRealtime, labelResult;
	private TweenHudLabel tweenRealtime, tweenResult;
	private TextBounds boundsRealtime, boundsResult;
	private DriftInfo drift;

	public HudDrifting( GameLogic logic )
	{
		this.logic = logic;
		this.player = logic.getGame().getLevel().getPlayer();
		this.model = player.getCarModel();
		carWidthPx = (int)Convert.mt2px( model.width );
		carLengthPx = (int)Convert.mt2px( model.length );

		labelRealtime = new HudLabel( Art.fontCurseYRbig, "99.99" );
		labelRealtime.setScale( 0.5f, true );
		labelRealtime.setAlpha( 0 );
		boundsRealtime = labelRealtime.getBounds();
		tweenRealtime = new TweenHudLabel( labelRealtime );

		labelResult = new HudLabel( Art.fontCurseR, "99.99" );
		labelResult.setAlpha( 0 );
		boundsResult = labelResult.getBounds();
		tweenResult = new TweenHudLabel( labelResult );
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

	public void render( SpriteBatch batch )
	{
		Vector2 p = Director.screenPosForPx( player.state().position );
		Vector2 h = player.getSimulator().heading;

		//
		// draw earned seconds
		//

		labelRealtime.setString( "+" + String.format( "%.02f", drift.driftSeconds ) );
		labelRealtime.setPosition( p.x - labelRealtime.getBounds().width * 0.5f, p.y - labelRealtime.getBounds().height * 0.5f );
		labelRealtime.x -= h.x * (carWidthPx + labelRealtime.getBounds().width * 0.5f);		// offset by heading
		labelRealtime.y -= h.y * (carLengthPx + labelRealtime.getBounds().height * 0.5f);		// offset by heading
		labelRealtime.render( batch );


		//
		// draw lost seconds
		//
		labelResult.setPosition( p.x - labelResult.getBounds().width * 0.5f, p.y - labelResult.getBounds().height * 0.5f );
		labelResult.x -= h.x * (carWidthPx + labelResult.getBounds().width * 0.5f);		// offset by heading
		labelResult.y -= h.y * (carLengthPx + labelResult.getBounds().height * 0.5f);		// offset by heading
		labelResult.render( batch );
	}

	public void onBeginDrift()
	{
		GameLogic.getTweener().add( Tween.to( tweenRealtime, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 1f ) );
	}

	public void onEndDrift()
	{
		if( drift.hasCollided )
		{
			// ended due to collision?
			labelResult.setString( "-" + String.format( "%.02f", drift.driftSeconds ) );
			labelResult.setFont( Art.fontCurseR );

			GameLogic.getTweener().add(
				TweenGroup.sequence(
					TweenGroup.parallel(
						Tween.to( tweenRealtime, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 0f ),
						Tween.to( tweenResult, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 1f )
					),
					Tween.call( new TweenCallback() {
						@Override
						public void tweenEventOccured( Types eventType, Tween tween )
						{
							GameLogic.getTweener().add( Tween.to( tweenResult, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 0f ) );
						}
					}).delay( 500 )
				)
			);
		}
		else
		{
			labelResult.setString( "+" + String.format( "%.02f", drift.driftSeconds ) );
			labelResult.setFont( Art.fontCurseG );

			GameLogic.getTweener().add(
				TweenGroup.sequence(
					TweenGroup.parallel(
						Tween.to( tweenRealtime, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 0f ),
						Tween.to( tweenResult, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 1f )
					),
					Tween.call( new TweenCallback() {
						@Override
						public void tweenEventOccured( Types eventType, Tween tween )
						{
							GameLogic.getTweener().add( Tween.to( tweenResult, TweenHudLabel.OPACITY, 500, Expo.INOUT ).target( 0f ) );
						}
					}).delay( 500 )
				)
			);

			if( drift.driftSeconds > 1 && drift.driftSeconds < 1.5f )
			{
				Messager.enqueue( "NICE ONE!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f,
						MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			} else if( drift.driftSeconds >= 1.5f && drift.driftSeconds < 2f )
			{
				Messager.enqueue( "FANTASTIC!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f,
						MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			} else if( drift.driftSeconds >= 2f )
			{
				Messager.enqueue( "U N R E A L!\n+" + String.format( "%.02f", drift.driftSeconds ) + "  seconds!", 1f,
						MessageType.Good, MessagePosition.Bottom, MessageSize.Big );
			}
		}
	}
}
