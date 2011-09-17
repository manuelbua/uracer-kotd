package com.bitfire.uracer.entities;


public strictfp abstract class SubframeInterpolableEntity extends Entity
{
	// world-coords
	protected EntityState statePrevious = new EntityState();
	protected EntityState stateCurrent = new EntityState();

	public abstract void saveStateTo( EntityState state );
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
			stateRender.set( EntityState.interpolate( statePrevious, stateCurrent, temporalAliasingFactor ) );
		}
		else
		{
			stateRender.set( stateCurrent );
		}
	}
}
