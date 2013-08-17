
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;

/** Encapsulates a set of one CarModel and one CarAspect, indicized by a single mnemonic, describing both physical and graphical
 * settings for the specified Type.
 * 
 * @author bmanuel */
public final class CarPreset {

	public Type type;
	public CarModel model = new CarModel();

	public enum Type {
		// @off
		Default("car"),
		Car("car"),
		Car_Yellow("car_yellow"),

//		L2_MustangRed("l2_mustang_red"), 
//		L2_MustangWhite("l2_mustang_white"), 
//		L2_BlueBeast("l2_blue_beast"), 
//		L2_PinkBeast("l2_pink_beast"), 
//		L2_RedBeast("l2_red_beast"), 
//		L2_YellowBeast("l2_yellow_beast"),
//
//		L3_Gallardo("l3_gallardo"),
//
//		Default("l1_minicooper"),
		;
		// @on

		public String regionName;

		private Type (String name) {
			regionName = name;
		}
	}

	public CarPreset (Type type) {
		setTo(type);
	}

	public void setTo (Type type) {
		switch (type) {
		// case L1_MiniCooper:
		// case L1_GoblinOrange:
		// model.toModel2();
		// model.width = 2.5f;
		// model.length = model.width * 1.6f;
		// break;
		//
		// case L2_MustangRed:
		// case L2_MustangWhite:
		// case L2_BlueBeast:
		// case L2_PinkBeast:
		// case L2_RedBeast:
		// case L2_YellowBeast:
		case Default:
		case Car_Yellow:
			model.toModel2();
			model.width = 2.4f;
			model.length = model.width * 1.72f;
			model.max_force = 300f;
			model.max_grip = 4.5f;
			model.friction = 8f;
			model.restitution = 0.35f;
			model.stiffness_rear = -3.8f; // rear cornering stiffness
			model.stiffness_front = -3.5f; // front cornering stiffness
			break;

		// case L3_Gallardo:
		// model.toModel2();
		// model.width = 2.5f;
		// model.length = 4.3f;
		// model.max_force = 400f;
		// model.max_grip = 5f;
		// model.friction = 8f;
		// break;

		default:
			Gdx.app.log("CarPreset", "No type definition available for \"" + type.toString() + "\"");
			break;
		}

		this.type = type;
		this.model.presetType = type;

	}
}
