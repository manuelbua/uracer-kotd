package com.bitfire.uracer.events;

public class GameLogicEvent {

	private final GameLogicNotifier notify = new GameLogicNotifier();

	public void addListener( GameLogicListener listener ) {
		notify.addListener( listener );
	}

	public final GameLogicListener trigger = new GameLogicListener() {
		@Override
		public void onRestart() {
			notify.onRestart();
		}

		@Override
		public void onReset() {
			notify.onReset();
		}
	};

}
