package co.rtapps.builder.model;

import java.net.URI;
import java.net.URISyntaxException;

public class DatabaseUrl {
	private String databaseUrl;
	private String jdbcUrl;
	private String fullJdbcUrl;
	private URI uri;
	private String username;
	private String password;
	private String host;
	private int port;
	private String db;
	
	public DatabaseUrl() {
		super();
	}
	
	public DatabaseUrl(boolean fromSysVar) throws URISyntaxException {
		super();
		
		if (fromSysVar)
			parseDatabseUrl(System.getenv("DATABASE_URL"));
		
	}

	public DatabaseUrl(String databaseUrl) throws URISyntaxException {
		super();
		
		parseDatabseUrl(databaseUrl);
		
	}
	
	private void parseDatabseUrl(String databaseUrl) throws URISyntaxException{

		this.uri = new URI(databaseUrl);
	    this.databaseUrl = databaseUrl;
	    this.username = uri.getUserInfo().split(":")[0];
	    this.password = uri.getUserInfo().split(":")[1];
	    this.jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ':' + uri.getPort() + uri.getPath() + "?sslmode=require";
	    this.fullJdbcUrl = "jdbc:postgresql://" + uri.getHost() + ':' + uri.getPort() + uri.getPath() + "?user=" + this.username + "&password=" + this.password + "sslmode=require";
	    this.host = uri.getHost();
	    this.port = uri.getPort();
	    this.db = uri.getPath();

	}
	
	public String getDatabaseUrl() {
		return databaseUrl;
	}
	public void setDatabaseUrl(String databaseUrl) throws URISyntaxException {
		this.databaseUrl = databaseUrl;
		parseDatabseUrl(this.databaseUrl);
	}
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	public URI getUri() {
		return uri;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
	public String getDb() {
		return db;
	}
	public String getFullJdbcUrl() {
		return fullJdbcUrl;
	}
}
