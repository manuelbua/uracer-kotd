package com.bitfire.uracer.game.world.models;

import java.util.List;

public class TrackWalls {
	public final List<OrthographicAlignedStillModel> models;
	private final boolean owned;

	public TrackWalls( List<OrthographicAlignedStillModel> models, boolean owned ) {
		this.models = models;
		this.owned = owned;
	}

	public int count() {
		return (models != null ? models.size() : 0);
	}

	public void dispose() {
		if( owned && models != null && models.size() > 0 ) {
			for( int i = 0; i < models.size(); i++ ) {
				models.get( i ).dispose();
			}

			models.clear();
		}
	}

}
