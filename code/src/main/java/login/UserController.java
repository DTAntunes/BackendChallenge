package login;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient.DebugTokenInfo;

import spark.Route;
import util.Configuration;

public class UserController {

	public static final Route CREATE_USER = (request, response) -> {
		DebugTokenInfo info = new DefaultFacebookClient(Configuration.APP_ACCESS_TOKEN,
		                                                Configuration.FB_API_VERSION).debugToken(request.queryParams("accessToken"));

		return info.isValid() + " " + info.getApplication() + " " + info.getMetaData();
	};

}
