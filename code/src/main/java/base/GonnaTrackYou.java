package base;

import spark.Spark;
import util.Configuration;
import util.Routes;
import util.StatusCodes;

/**
 * Sets up the routes and behaviour for the GonnaTrackYou application, which
 * lets a user opt into having their data harvested so we can know if they're
 * popular, go places, and like music.
 *
 * @author Dário T A
 */
public class GonnaTrackYou {

	public static final int SPARK_PORT = 12345;

	public static void main(String[] args) {
		startServer(SPARK_PORT);
	}

	public static void startServer(int port) {
		// Set up the webserver, this automatically starts it as well
		Spark.port(port);

		// Set up the routes
		Routes.LOGIN.addPost();
		Routes.DATA.addGet();

		Spark.after((request, response) -> {
			response.type("application/json");
			// this is needed due to the way the testing HttpClient responds to a 401
			if (Configuration.TESTING
			    && response.status() == StatusCodes.ClientError.UNAUTHORIZED) {
				response.header("WWW-Authenticate", "ReadTheDocs realm=\"none\"");
			}
		});
	}

}
