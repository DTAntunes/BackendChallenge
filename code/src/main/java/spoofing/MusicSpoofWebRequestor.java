package spoofing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.restfb.BinaryAttachment;
import com.restfb.DebugHeaderInfo;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.WebRequestor;
import com.restfb.json.JsonObject;

import util.Configuration;

public class MusicSpoofWebRequestor implements WebRequestor {

	private static final HashMap<String, String> RESPONSES_BY_ID = new HashMap<>();
	static {
		ClassLoader loader = MusicSpoofWebRequestor.class.getClassLoader();
		InputStream input = loader.getResourceAsStream("musicSpoofing");
		if (input != null) {
			Scanner scan = new Scanner(input);
			while (scan.hasNextLine()) {
				String id = scan.nextLine();
				String response = scan.nextLine();
				RESPONSES_BY_ID.put(id, response);
			}
			scan.close();
		}
	}

	private static String extractAccessToken(String url) {
		for (NameValuePair pair : URLEncodedUtils.parse(url.substring(url.indexOf('?') + 1),
		                                                Charset.defaultCharset())) {
			if (pair.getName().equalsIgnoreCase("access_token")) {
				return pair.getValue();
			}
		}
		return null;
	}

	private WebRequestor actualRequestor = new DefaultWebRequestor();

	@Override
	public Response executeDelete(String url) throws IOException {
		return actualRequestor.executeDelete(url);
	}

	@Override
	public Response executeGet(String url) throws IOException {
		// naive as anything but it works for now
		if (url.contains("me/music")) {
			String accessToken = extractAccessToken(url);
			if (accessToken == null || accessToken.isEmpty()) {
				// let the actual thing deal with the error
				return actualRequestor.executeGet(url);
			}
			String userId = new DefaultFacebookClient(accessToken,
			                                          Configuration.FB_API_VERSION).fetchObject("me",
			                                                                                    JsonObject.class)
			                                                                       .getString("id");
			if (RESPONSES_BY_ID.containsKey(userId)) {
				return new Response(200, RESPONSES_BY_ID.get(userId));
			} else {
				return actualRequestor.executeGet(url);
			}
		} else {
			return actualRequestor.executeGet(url);
		}
	}

	@Override
	public Response executePost(String url, String parameters) throws IOException {
		return actualRequestor.executePost(url, parameters);
	}

	@Override
	public Response executePost(String url, String parameters,
	                            BinaryAttachment... binaryAttachments) throws IOException {
		return actualRequestor.executePost(url, parameters, binaryAttachments);
	}

	@Override
	public DebugHeaderInfo getDebugHeaderInfo() {
		return actualRequestor.getDebugHeaderInfo();
	}

}
