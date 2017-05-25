package login.responseModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import login.PermissionGrant;
import login.UserModel;
import util.ResourceObject;

public class CreateResponse {
	
	public final ResourceObject[] data;
	public final Map<String, Object> meta = new HashMap<>();
	
	public CreateResponse(ArrayList<PermissionGrant> permissions, UserModel user) {
		data = new ResourceObject[permissions.size()];
		
		for (int i = 0; i < data.length; i++) {
			data[i] = new ResourceObject(permissions.get(0).permission, "permission");
		}
		meta.put("accessInfo", user);
	}
	
	public CreateResponse(UserModel user) {
		this(new ArrayList<>(), user);
	}

}
