package login.responseModels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import login.LoginModel;
import util.ResourceObject;

public class CreateResponse {
	
	public final ResourceObject[] data;
	public final Map<String, Object> meta = new HashMap<>();
	
	public CreateResponse(HashSet<String> permissions, LoginModel user) {
		data = new ResourceObject[permissions.size()];
		permissions.toArray(data);
		meta.put("accessInfo", user);
	}
	
	public CreateResponse(LoginModel user) {
		this(new HashSet<>(), user);
	}

}
