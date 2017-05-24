import spark.Spark;
import util.Routes;

/**
 * Sets up the routes and behaviour for the GonnaTrackYou application, which
 * lets a user opt into having their data harvested so we can know if they're
 * popular, go places, and like music.
 *
 * @author Dário T A
 */
public class GonnaTrackYou {

	private static int SPARK_PORT = 12345;

	public static void main(String[] args) {
		// Set up the webserver, this automatically starts it as well
		Spark.port(SPARK_PORT);

		// Set up the routes
		Routes.CREATE_USER.addPost();
	}

}
