package util;

import com.google.gson.Gson;

public class JsonRenderer {
	
	private static final Gson RENDERER = new Gson();
	
	public static String render(Object response) {
		return RENDERER.toJson(response);
	}

}
