package login.responseModels;

public class LocationPreference extends ErrorableData {

	public final String place;
	public final Integer count;

	public LocationPreference(String place, Integer count, boolean error) {
		this.error = error;
		if (error) {
			this.place = null;
			this.count = null;
		} else {
			this.place = place;
			this.count = count;
		}
	}

}
