package io.terpomo.pmitz.core.subjects;

public class IndividualUser extends UserGrouping {

	private String userId;

	public IndividualUser(String userId) {
		this.userId = userId;
	}


	@Override
	public String getId() {
		return userId;
	}
}
