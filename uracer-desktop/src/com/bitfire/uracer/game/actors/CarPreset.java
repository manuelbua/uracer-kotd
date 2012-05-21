package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;

/** Encapsulates a set of one CarModel and one CarAspect, indicized by a single mnemonic, describing both physical and
 * graphical settings for the specified Type.
 *
 * @author bmanuel */
public final class CarPreset {

	public Type type;
	public CarModel model = new CarModel();

	public enum Type {
		// @formatter:off
		Default( "electron" ),
		Model1( "electron" ),
		Model2( "spider" ),
		AudiTTSCoupe2011( "audi-tts-coupe-2011" ),
		FordMustangShelbyGt500Coupe( "ford-mustang-shelby-gt500-coupe" ),
		FordMustangShelbyGt500White( "ford-mustang-shelby-gt500-white" ),
		LamborghiniGallardoLP560( "lamborghini-gallardo-lp560" ),
		CooperMiniCoupe( "cooper-mini-coupe" ),
		SportCar( "sport-car" ),
		SportMasek( "sport-masek" ),
		SportStella( "sport-stella" ),
		;
		// @formatter:on

		public String regionName;

		private Type( String name ) {
			regionName = name;
		}
	}

	public CarPreset( Type type ) {
		setTo( type );
	}

	public void setTo( Type type ) {
		switch( type ) {
		case Default:
			model.toDefault();
			break;

		case CooperMiniCoupe:
			model.toModel2();
			model.width = 2.5f;
			model.length = model.width * 1.6f;
			break;

		case FordMustangShelbyGt500White:
		case FordMustangShelbyGt500Coupe:
			model.toModel2();
			model.width = 2.5f;
			model.length = 4.3f;
			model.max_force = 350f;
			model.max_grip = 6f;
			break;

		case LamborghiniGallardoLP560:
			model.toModel2();
			model.width = 2.5f;
			model.length = 4.3f;
			model.max_force = 400f;
			model.max_grip = 5f;
			model.friction = 8f;
			break;

		case AudiTTSCoupe2011:
			model.toModel2();
			model.width = 2.5f;
			model.length = 4.5f;
			break;

		case SportCar:
			model.toModel2();
			model.width = 2.5f;
			model.length = 4.3f;
			break;

		case SportMasek:
			model.toModel2();
			model.width = 2.5f;
			model.length = 3.85f;
			break;

		case SportStella:
			model.toModel2();
			model.width = 2.5f;
			model.length = 4.3f;
			break;

		default:
			Gdx.app.log( "CarPreset", "No type definition available for \"" + type.toString() + "\"" );
			break;
		}

		this.type = type;
		this.model.presetType = type;

	}
}
