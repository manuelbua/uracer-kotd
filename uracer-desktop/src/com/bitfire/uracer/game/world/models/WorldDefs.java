
package com.bitfire.uracer.game.world.models;

public final class WorldDefs {

	/** Attributes for Tiled's object properties. */
	public enum ObjectProperties {
		// @formatter:off
		MeshScale("scale");
		// @formatter:on

		public final String mnemonic;

		private ObjectProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's world tile layers */
	public enum TileLayer {
		// @formatter:off
		Track("track");
		// @formatter:on

		public final String mnemonic;

		private TileLayer (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's world object layer/group */
	public enum ObjectGroup {
		// @formatter:off
		Lights("lights"), StaticMeshes("static-meshes"), Trees("trees"), Walls("walls");
		// @formatter:on

		public final String mnemonic;

		private ObjectGroup (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's layer properties */
	public enum LayerProperties {
		// @formatter:off
		Start("start");
		// @formatter:on
		public final String mnemonic;

		private LayerProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}

	}

	/** Tiled's tile properties */
	public enum TileProperties {
		// @formatter:off
		Type("type");
		// @formatter:on
		public final String mnemonic;

		private TileProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}

	}
}
