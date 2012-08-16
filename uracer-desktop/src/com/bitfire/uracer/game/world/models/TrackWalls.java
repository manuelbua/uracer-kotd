
package com.bitfire.uracer.game.world.models;

import java.util.List;

public class TrackWalls {
	public final List<OrthographicAlignedStillModel> models;

	public TrackWalls (List<OrthographicAlignedStillModel> models) {
		this.models = models;
	}

	public int count () {
		return (models != null ? models.size() : 0);
	}
}
