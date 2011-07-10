package com.bitfire.uracer.screen;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Physics;

public class TestRope
{
	private World world;
	private int segments;
	private Body ground;
	private Array<Body> ropeSegments = new Array<Body>();
	private SpriteBatch batch;
	protected ImmediateModeRenderer20 renderer = new ImmediateModeRenderer20( false, true, 0 );
	private Sprite sprite;
	private Texture tex;

	TestRope( World world, int segments, Body ground )
	{
		this.world = world;
		this.segments = segments;
		this.ground = ground;
		this.batch = new SpriteBatch(100);

		PolygonShape shape = new PolygonShape();
		shape.setAsBox( 0.5f, 0.125f );
		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 20.0f;
		fd.friction = 0.2f;

		RevoluteJointDef jd = new RevoluteJointDef();

		Body prevBody = ground;

		for( int i = 0; i < this.segments; i++ )
		{
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set( -14.5f + 1.0f * i, 5.0f );
			Body body = world.createBody( bd );
			body.createFixture( fd );

			Vector2 anchor = new Vector2( -15.0f + 1.0f * i, 5.0f );
			jd.initialize( prevBody, body, anchor );
			world.createJoint( jd );
			prevBody = body;
			ropeSegments.add( body );
		}

		Vector2 anchor = new Vector2( -15.0f + 1.0f * this.segments, 5.0f );
		jd.initialize( prevBody, ground, anchor );
		world.createJoint( jd );
		shape.dispose();

		sprite = new Sprite();
		sprite.setRegion( Art.rope );
		sprite.setSize( Physics.w2s( 1 ), Physics.w2s( 0.25f ) );
		sprite.setOrigin( sprite.getWidth()/2, sprite.getHeight()/2 );
	}

	Vector2 pos = new Vector2();
	public void render( Camera camera )
	{
		batch.setProjectionMatrix( camera.projection );
		batch.setTransformMatrix( camera.view );

		batch.disableBlending();
		batch.begin();

		for( int i = 0; i < ropeSegments.size; i++ )
		{
			Body body = ropeSegments.get( i );
			pos = body.getPosition();
			pos = Physics.w2s( pos );
			sprite.setPosition( pos.x-sprite.getOriginX(), pos.y-sprite.getOriginY() );
			sprite.setRotation( body.getAngle()*MathUtils.radiansToDegrees );
			sprite.draw( batch, 0.4f );
		}

		batch.end();
	}
}
