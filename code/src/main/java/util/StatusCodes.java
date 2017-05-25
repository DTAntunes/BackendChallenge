package util;

public class StatusCodes {

	public static class ClientError {

		public static final int UNAUTHORIZED = 401, FORBIDDEN = 403, NOT_FOUND = 404;
	}

	public static class Success {

		public static final int OK = 200, CREATED = 201, ACCEPTED = 202, NO_CONTENT = 204;
	}

}
