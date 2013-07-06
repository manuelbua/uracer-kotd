
package com.bitfire.uracer.game.logic.gametasks.trackeffects.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffectType;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class PlayerSkidMarks extends TrackEffect {
	// public static final int MaxSkidMarks = 5000;
	// private static final float MaxParticleLifeSeconds = 60f;

	private final int MaxSkidMarks;
	private final float MaxParticleLifeSeconds;
	private final float InvMaxParticleLifeSeconds;

	private SkidMark[] skidMarks;
	private int markIndex;
	private int visibleSkidMarksCount;
	// private int driftMarkAddIterations = 1;
	private Vector2 pos, last;

	public PlayerSkidMarks (int maxSkidMarks, float maxParticleLifeSecs) {
		super(TrackEffectType.CarSkidMarks);

		MaxSkidMarks = maxSkidMarks;
		MaxParticleLifeSeconds = maxParticleLifeSecs;
		InvMaxParticleLifeSeconds = 1f / MaxParticleLifeSeconds;

		markIndex = 0;
		visibleSkidMarksCount = 0;

		pos = new Vector2();
		last = new Vector2();

		skidMarks = new SkidMark[MaxSkidMarks];
		for (int i = 0; i < MaxSkidMarks; i++) {
			skidMarks[i] = new SkidMark();
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
	public void player (PlayerCar player) {
		super.player(player);
		if (hasPlayer) {
			for (int i = 0; i < MaxSkidMarks; i++) {
				skidMarks[i].setup(Convert.mt2px(player.getCarModel().width), Convert.mt2px(player.getCarModel().length));
				// skidMarks[i].life = 0;
			}
		}
	}

	@Override
	public int getParticleCount () {
		return visibleSkidMarksCount;
	}

	@Override
	public int getMaxParticleCount () {
		return MaxSkidMarks;
	}

	@Override
	public void dispose () {
	}

	@Override
	public void reset () {
		markIndex = 0;
		visibleSkidMarksCount = 0;
	}

	private Vector2 ppos = new Vector2();

	@Override
	public void tick () {
		if (hasPlayer) {
			if (player.carState.currVelocityLenSquared >= 1 && player.driftState.driftStrength > 0.3f
				&& player.carState.currSpeedFactor > 0.1f) {
				ppos.x = Convert.mt2px(player.getBody().getPosition().x);
				ppos.y = Convert.mt2px(player.getBody().getPosition().y);
				tryAddDriftMark(ppos, player.state().orientation);
			}
		}

		for (int i = 0; i < MaxSkidMarks; i++) {
			SkidMark sm = skidMarks[i];
			if (sm.life > 0) {
				sm.life -= Config.Physics.Dt;
			} else {
				sm.life = 0;
			}
		}
	}

	@Override
	public void render (SpriteBatch batch) {
		float lifeRatio;
		SkidMark d;
		visibleSkidMarksCount = 0;

		// front drift marks
		for (int i = 0; i < MaxSkidMarks; i++) {
			d = skidMarks[i];
			if (d.life > 0 && GameRenderer.ScreenUtils.isVisible(d.getBoundingRectangle())) {
				visibleSkidMarksCount++;

				lifeRatio = d.life * InvMaxParticleLifeSeconds;

				d.front.setColor(1, 1, 1, d.alphaFront * lifeRatio);
				d.rear.setColor(1, 1, 1, d.alphaRear * lifeRatio);

				d.front.draw(batch);
				d.rear.draw(batch);
			}
		}

		// Gdx.app.log( "PlayerSkidMarks", "visibles=" + visibleSkidMarksCount );
	}

	private void tryAddDriftMark (Vector2 position, float orientation) {
		// avoid blatant overdrawing
		if ((int)position.x == (int)last.x && (int)position.y == (int)last.y) {
			return;
		}

		int driftMarkAddIterations = 1;
		float target = Config.Physics.Dt;
		float curr = Gdx.graphics.getDeltaTime();// deltaMean.getMean();
		driftMarkAddIterations = AMath.clamp(Math.round(curr / target), 1, 3);

		float theta = 1f / (float)driftMarkAddIterations;
		for (int i = 0; i < driftMarkAddIterations; i++) {
			pos.set(position);

			pos.x = AMath.lerp(last.x, position.x, theta * i);
			pos.y = AMath.lerp(last.y, position.y, theta * i);

			// add front drift marks?
			SkidMark drift = skidMarks[markIndex++];
			if (markIndex == MaxSkidMarks) {
				markIndex = 0;
			}

			drift.alphaFront = player.driftState.lateralForcesFront * player.driftState.driftStrength * theta;
			drift.alphaRear = player.driftState.lateralForcesRear * player.driftState.driftStrength * theta;
			drift.setPosition(pos);
			drift.setOrientation(orientation);

			drift.life = MaxParticleLifeSeconds;
		}

		last.set(position);

		// Gdx.app.log( "PlayerSkidMarks", NumberString.format(driftState.driftStrength) + "/" +
		// NumberString.format(driftState.lateralForcesFront) );
	}

	private class SkidMark {
		public Sprite front, rear;
		public float life;
		public float alphaFront, alphaRear;

		public SkidMark () {
			front = new Sprite();
			rear = new Sprite();
			life = MaxParticleLifeSeconds;
		}

		public void setup (float carWidthPx, float carLengthPx) {
			front.setRegion(Art.skidMarksFront);
			front.setSize(carWidthPx, carLengthPx);
			front.setOrigin(front.getWidth() / 2, front.getHeight() / 2);
			front.setColor(1, 1, 1, 1);

			rear.setRegion(Art.skidMarksRear);
			rear.setSize(carWidthPx, carLengthPx);
			rear.setOrigin(rear.getWidth() / 2, rear.getHeight() / 2 + 7); // adjust for rear axis in 3d model (pretty distant)
			rear.setColor(1, 1, 1, 1);
		}

		public void setPosition (Vector2 pos) {
			front.setPosition(pos.x - front.getOriginX(), pos.y - front.getOriginY());
			rear.setPosition(pos.x - rear.getOriginX(), pos.y - rear.getOriginY());
		}

		public void setOrientation (float degrees) {
			front.setRotation(degrees);
			rear.setRotation(degrees);
		}

		public Rectangle getBoundingRectangle () {
			return front.getBoundingRectangle();
		}
	}

}
