package com.bitfire.uracer.game.logic.trackeffects.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.trackeffects.TrackEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftState;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class PlayerSkidMarks extends TrackEffect {
	public static final int MaxSkidMarks = 300;
	private static final float MaxParticleLifeSeconds = 3.5f;
	private static final float InvMaxParticleLifeSeconds = 1f / MaxParticleLifeSeconds;

	private SkidMark[] skidMarks;
	private int markIndex;
	private int visibleSkidMarksCount;
	// private int driftMarkAddIterations = 1;

	private Vector2 pos, last;
	private PlayerCar player;

	public PlayerSkidMarks( PlayerCar player ) {
		super( Type.CarSkidMarks );

		this.player = player;
		markIndex = 0;
		visibleSkidMarksCount = 0;

		pos = new Vector2();
		last = new Vector2();

		skidMarks = new SkidMark[ MaxSkidMarks ];
		for( int i = 0; i < MaxSkidMarks; i++ ) {
			skidMarks[i] = new SkidMark( Convert.mt2px( player.getCarModel().width ), Convert.mt2px( player.getCarModel().length ) );
		}

		// 1 iteration at 60Hz, 2 at 30Hz..
		// if( Config.Physics.PhysicsTimestepHz > 60 ) {
		// driftMarkAddIterations = 0;
		// Gdx.app.log( "PlayerSkidMarks", "Physics timestep is too small, giving up the effect." );
		// } else {
		// driftMarkAddIterations = (int)(60 / (int)Config.Physics.PhysicsTimestepHz);
		// }
	}

	@Override
	public int getParticleCount() {
		return visibleSkidMarksCount;
	}

	@Override
	public void dispose() {
		for( int i = 0; i < MaxSkidMarks; i++ ) {
			skidMarks[i] = null;
		}
	}

	@Override
	public void reset() {
		markIndex = 0;
	}

	private Vector2 ppos = new Vector2();

	@Override
	public void tick() {
		if( player.carState.currVelocityLenSquared >= 1 ) {
			ppos.x = Convert.mt2px( player.getBody().getPosition().x );
			ppos.y = Convert.mt2px( player.getBody().getPosition().y );

			// tryAddDriftMark( player.state().position, player.state().orientation, player.driftState );
			tryAddDriftMark( ppos, player.state().orientation, player.driftState );
		}

		SkidMark d;
		for( int i = 0; i < MaxSkidMarks; i++ ) {
			d = skidMarks[i];
			if( d.life > 0 ) {
				d.life -= Config.Physics.PhysicsDt;
			} else {
				d.life = 0;
			}
		}
	}

	@Override
	public void render( SpriteBatch batch ) {
		float lifeRatio;
		SkidMark d;
		visibleSkidMarksCount = 0;

		// front drift marks
		for( int i = 0; i < MaxSkidMarks; i++ ) {
			d = skidMarks[i];
			if( d.life > 0 && GameRenderer.ScreenUtils.isVisible( d.getBoundingRectangle() ) ) {
				visibleSkidMarksCount++;

				lifeRatio = d.life * InvMaxParticleLifeSeconds;

				d.front.setColor( 1, 1, 1, d.alphaFront * lifeRatio );
				d.rear.setColor( 1, 1, 1, d.alphaRear * lifeRatio );

				d.front.draw( batch );
				d.rear.draw( batch );
			}
		}

		// Gdx.app.log( "PlayerSkidMarks", "visibles=" + visibleSkidMarksCount );
	}

	private void tryAddDriftMark( Vector2 position, float orientation, PlayerDriftState driftState ) {
		// avoid blatant overdrawing
		if( (int)position.x == (int)last.x && (int)position.y == (int)last.y ) {
			// position.x = AMath.lerp( last.x, position.x, 0.25f );
			// position.y = AMath.lerp( last.y, position.y, 0.25f );
			// Gdx.app.log( "PlayerSkidMarks", "Skipping emitting particle..." );
			return;
		}

		int driftMarkAddIterations = 1;
		float target = Config.Physics.PhysicsDt;
		float curr = Gdx.graphics.getDeltaTime();// deltaMean.getMean();
		driftMarkAddIterations = AMath.clamp( Math.round( curr / target ), 1, 3 );

		float theta = 1f / (float)driftMarkAddIterations;
		for( int i = 0; i < driftMarkAddIterations; i++ ) {
			pos.set( position );

			pos.x = AMath.lerp( last.x, position.x, theta * i );
			pos.y = AMath.lerp( last.y, position.y, theta * i );

			if( driftState.driftStrength > 0.2f )
			// if( di.isDrifting )
			{
				// add front drift marks?
				SkidMark drift = skidMarks[markIndex++];
				if( markIndex == MaxSkidMarks ) {
					markIndex = 0;
				}

				// drift.alphaFront = driftState.driftStrength;
				// drift.alphaRear = driftState.driftStrength;
				drift.alphaFront = driftState.lateralForcesFront * driftState.driftStrength * theta;
				drift.alphaRear = driftState.lateralForcesRear * driftState.driftStrength * theta;
				drift.setPosition( pos );
				drift.setOrientation( orientation );
				drift.front.setScale( AMath.clamp( driftState.lateralForcesFront + 0.25f, 0.75f, 1.0f ) );
				drift.rear.setScale( AMath.clamp( driftState.lateralForcesRear + 0.25f, 0.75f, 1.0f ) );
				drift.life = MaxParticleLifeSeconds;
			}
		}

		last.set( position );

		// Gdx.app.log( "PlayerSkidMarks", NumberString.format(driftState.driftStrength) + "/" +
		// NumberString.format(driftState.lateralForcesFront) );
	}

	private class SkidMark {
		public Sprite front, rear;
		public float life;
		public float alphaFront, alphaRear;

		public SkidMark( float carWidthPx, float carLengthPx ) {
			front = new Sprite();
			rear = new Sprite();

			front.setRegion( Art.skidMarksFront );
			front.setSize( carWidthPx, carLengthPx );
			front.setOrigin( front.getWidth() / 2, front.getHeight() / 2 );
			front.setColor( 1, 1, 1, 1 );

			rear.setRegion( Art.skidMarksRear );
			rear.setSize( carWidthPx, carLengthPx );
			rear.setOrigin( rear.getWidth() / 2, rear.getHeight() / 2 );
			rear.setColor( 1, 1, 1, 1 );

			life = MaxParticleLifeSeconds;
		}

		public void setPosition( Vector2 pos ) {
			front.setPosition( pos.x - front.getOriginX(), pos.y - front.getOriginY() );
			rear.setPosition( pos.x - rear.getOriginX(), pos.y - rear.getOriginY() );
		}

		public void setOrientation( float degrees ) {
			front.setRotation( degrees );
			rear.setRotation( degrees );
		}

		private Rectangle tmp = new Rectangle();

		public Rectangle getBoundingRectangle() {
			// front and rear rectangles always converge, just use one
			tmp.set( front.getBoundingRectangle() );
			// tmp.merge( rear.getBoundingRectangle() );
			return tmp;
		}
	}

}
