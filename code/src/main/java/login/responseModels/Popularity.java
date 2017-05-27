package login.responseModels;

public class Popularity extends ErrorableData {

	private static final int MAX_FRIENDS = 5000;
	private static final int POPULAR_FRIEND_THRESHOLD = 50;

	public final Boolean popular;
	public final Long popularMeasure;

	public Popularity(Long friendCount, boolean error) {
		this.error = error;
		if (error) {
			popular = null;
			popularMeasure = null;
		} else {
			popular = friendCount >= POPULAR_FRIEND_THRESHOLD;
			popularMeasure = friendCount * 100 / MAX_FRIENDS;
		}
	}

}
