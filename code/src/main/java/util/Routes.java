package util;

import login.UserController;
import spark.Route;
import spark.Spark;

/**
 * An abstraction to more tightly couple routes to paths relative to how Spark
 * prefers to do it.
 *
 * @author Dário T A
 */
public enum Routes {

	CREATE_USER(Paths.Login.CREATE, UserController.CREATE_USER);

	// These are both immutable, so public final is good enough
	public final String path;
	public final Route callback;

	private Routes(String path, Route callback) {
		this.path = path;
		this.callback = callback;
	}

	public void addPost() {
		Spark.post(path, callback);
	}

}
