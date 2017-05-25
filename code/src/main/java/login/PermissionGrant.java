package login;

public class PermissionGrant {

	public String permission;
	public String status;

	public PermissionGrant(String permission, String status) {
		this.permission = permission;
		this.status = status;
	}

	public String getPermission() {
		return permission;
	}

	public String getStatus() {
		return status;
	}
}