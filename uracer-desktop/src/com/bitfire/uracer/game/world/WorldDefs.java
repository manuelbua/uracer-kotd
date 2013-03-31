
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
	public enum Layer {
		// @off
		Track("track"),
//		Lights("lights"), 
//		StaticMeshes("static-meshes"), 
//		Trees("trees"), 
//		Walls("walls"),
//		Route("route"),
//		Sectors("sectors")
		;
		// @on

		public final String mnemonic;

		private Layer (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	/** Tiled's world object layer/group */
	public enum ObjectGroup {
		// @off
		Lights("lights"), 
		StaticMeshes("static-meshes"), 
		Trees("trees"), 
		Walls("walls"),
		Route("route"),
		Sectors("sectors")
		;
		// @on

		public final String mnemonic;

		private ObjectGroup (String mnemonic) {
			this.mnemonic = mnemonic;
		}
	}

	// /** Tiled's layer properties */
	// public enum LayerProperties {
//		// @off
//		None("");
//		// @on
	// public final String mnemonic;
	//
	// private LayerProperties (String mnemonic) {
	// this.mnemonic = mnemonic;
	// }
	// }
	//
	// /** Tiled's tile properties */
	// public enum TileProperties {
//		// @off
//		None("");
//		// @on
	// public final String mnemonic;
	//
	// private TileProperties (String mnemonic) {
	// this.mnemonic = mnemonic;
	// }
	// }
}
