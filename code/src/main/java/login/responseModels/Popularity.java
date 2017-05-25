package login.responseModels;

public class Popularity extends ErrorableData {
	
	private static final int MAX_FRIENDS = 5000;
	private static final int POPULAR_FRIEND_THRESHOLD = 50;

	public final Boolean popular;
	public final Long popularMeasure;

	public Popularity(Long friendCount, boolean error) {
		this.error = error;
		if (error) {
			this.popular = null;
			this.popularMeasure = null;
		} else {
			this.popular = friendCount > POPULAR_FRIEND_THRESHOLD;
			this.popularMeasure = friendCount * 100 / MAX_FRIENDS;
		}
	}
	
}
