
package com.bitfire.uracer.configuration;

public final class UserProfile {

	public enum Nation {
		None
	}

	public final long userId;
	public final String userName;
	public final Nation userNationId;

	public UserProfile () {
		userId = -1;
		userName = "Manuel";
		userNationId = Nation.None;
	}
}
