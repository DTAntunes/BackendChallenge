package login;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.restfb.ConnectionIterator;
import com.restfb.json.JsonObject;
import com.restfb.types.Location;
import com.restfb.types.PlaceTag;

import login.responseModels.LocationPreference;
import login.responseModels.MusicPreference;
import util.Configuration;

public class UserDataUtility {

	public static MusicPreference
	       getPreferredBand(ConnectionIterator<JsonObject> musicData) throws ParseException {
		String oldestPageName = null;
		Date oldestPageTime = new Date(Integer.MAX_VALUE);

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");
		while (musicData.hasNext()) {
			for (JsonObject p : musicData.next()) {
				try {
					Date createdTime = formatter.parse(p.getString("created_time"));
					if (oldestPageName == null || createdTime.before(oldestPageTime)) {
						oldestPageName = p.getString("name");
						oldestPageTime = createdTime;
					}
				} catch (ParseException e) {
					/*
					 * I'm making the likely flawed assumption that they won't
					 * change their timestamp format. There should be some sort
					 * of logging call here, but I haven't set logging up for
					 * this, so let's just prevent a change here from causing a
					 * 500 error on the whole endpoint
					 */
					if (Configuration.TESTING) {
						// but just in case, let's break it in testing so it's
						// easily caught
						throw e;
					}
				}
			}
		}

		if (oldestPageName == null) {
			return new MusicPreference(null, null, false);
		} else {
			return new MusicPreference(oldestPageName, formatter.format(oldestPageTime), false);
		}
	}

	public static LocationPreference getPreferredLocation(ConnectionIterator<PlaceTag> placeData) {
		HashMap<String, Integer> placeCount = new HashMap<>();
		String maxPlace = null;
		int maxCount = 0;

		while (placeData.hasNext()) {
			for (PlaceTag place : placeData.next()) {
				Location loc = place.getPlace().getLocation();
				String city;
				if (loc != null && (city = qualifyName(loc)) != null) {
					Integer currentCount;
					if ((currentCount = placeCount.get(city)) == null) {
						currentCount = 1;
					} else {
						currentCount++;
					}

					if (currentCount > maxCount) {
						maxPlace = city;
						maxCount = currentCount;
					}

					placeCount.put(city, currentCount);
				}
			}
		}

		return new LocationPreference(maxPlace, maxCount, false);
	}

	private static String qualifyName(Location loc) {
		if (loc.getCity() != null && loc.getCountry() != null) {
			return loc.getCity() + ", " + loc.getCountry();
		}
		return null;
	}

}
