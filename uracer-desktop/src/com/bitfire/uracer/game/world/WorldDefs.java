
package com.bitfire.uracer.game.world;

public final class WorldDefs {

	/** Attributes for Tiled's object properties. */
	public enum ObjectProperties {
		// @off
		MeshScale("scale");
		// @on

		public final String mnemonic;

		private ObjectProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's world tile layers */
	public enum TileLayer {
		// @off
		Track("track");
		// @on

		public final String mnemonic;

		private TileLayer (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's world object layer/group */
	public enum ObjectGroup {
		// @off
		Lights("lights"), StaticMeshes("static-meshes"), Trees("trees"), Walls("walls");
		// @on

		public final String mnemonic;

		private ObjectGroup (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's layer properties */
	public enum LayerProperties {
		// @off
		Start("start");
		// @on
		public final String mnemonic;

		private LayerProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}

	}

	/** Tiled's tile properties */
	public enum TileProperties {
		// @off
		Type("type");
		// @on
		public final String mnemonic;

		private TileProperties (String mnemonic) {
			this.mnemonic = mnemonic;
		}

	}
}
