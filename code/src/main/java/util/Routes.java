package util;

import login.UserController;
import spark.Route;
import spark.Spark;

/**
 * An abstraction to more tightly couple routes to paths relative to how Spark prefers to do it.
 *
 * @author Dário T A
 */
public enum Routes {

	LOGIN(Paths.LOGIN, UserController.LOGIN), DATA(Paths.GET_DATA, UserController.GET_DATA);

	// These are both immutable, so public final is good enough
	public final String path;
	public final Route callback;

	private Routes(String path, Route callback) {
		this.path = path;
		this.callback = callback;
	}

	public void addGet() {
		Spark.get(path, callback);
	}

	public void addPost() {
		Spark.post(path, callback);
	}

}
