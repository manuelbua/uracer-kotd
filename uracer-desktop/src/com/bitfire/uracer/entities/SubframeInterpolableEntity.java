package com.bitfire.uracer.entities;

public abstract class SubframeInterpolableEntity extends Entity
{
	protected EntityScreenState statePrevious = new EntityScreenState();
	protected EntityScreenState stateCurrent = new EntityScreenState();

	public abstract void saveStateTo( EntityScreenState state );
	public abstract boolean isSubframeInterpolated();

	protected void resetState()
	{
		saveStateTo( stateCurrent );
		statePrevious.set( stateCurrent );
		stateRender.set( stateCurrent );
	}

	public void onBeforePhysicsSubstep()
	{
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep()
	{
		saveStateTo( stateCurrent );
	}

	public void onBeforeRender( float temporalAliasingFactor )
	{
		if( isSubframeInterpolated() )
		{
			stateRender.set( EntityScreenState.interpolate( statePrevious, stateCurrent, temporalAliasingFactor ) );
		}
		else
		{
			stateRender.set( stateCurrent );
		}
	}
}
