package login.responseModels;

public class MusicPreference extends ErrorableData {

	public final String band;
	public final String likeDate;

	public MusicPreference(String band, String likeDate, boolean error) {
		this.error = error;
		if (error) {
			this.band = null;
			this.likeDate = null;
		} else {
			this.band = band;
			this.likeDate = likeDate;
		}
	}
}
