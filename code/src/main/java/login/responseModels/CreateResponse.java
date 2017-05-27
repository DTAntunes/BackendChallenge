package login.responseModels;

import java.util.HashMap;
import java.util.HashSet;

import login.LoginModel;
import util.ResourceObject;

public class CreateResponse {

	public final ResourceObject[] data;
	public final HashMap<String, Object> meta = new HashMap<>();

	public CreateResponse(HashSet<String> permissions, LoginModel user) {
		data = new ResourceObject[permissions.size()];
		int index = 0;
		for (String s : permissions) {
			data[index++] = new ResourceObject(s, "permission");
		}
		meta.put("accessInfo", user);
	}

	public CreateResponse(LoginModel user) {
		this(new HashSet<>(), user);
	}

}
