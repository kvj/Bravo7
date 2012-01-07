package org.kvj.bravo7.data;

public interface DataAccessService {
	
	public String getLoginURL();
	public String getAccessToken(String code);
	public String getAccessToken(String username, String password);
}
