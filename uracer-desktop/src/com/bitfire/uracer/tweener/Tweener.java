package com.bitfire.uracer.tweener;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;
import aurelienribon.tweenengine.TweenManager;

public class Tweener
{
	private TweenManager manager;

	public Tweener()
	{
		Tween.enablePooling( true );
		manager = new TweenManager();
	}

	public void dispose()
	{

	}

	public static void registerAccessor( Class someClass, TweenAccessor accessor)
	{
		Tween.registerAccessor( someClass, accessor );
	}

	public void clear()
	{
		manager.killAll();
	}

	public void start(Timeline timeline)
	{
		timeline.start( manager );
	}

	public void update(int deltaMillis)
	{
		manager.update( deltaMillis );
	}
}
