package com.ig.testdrive.integration.solr.beans;

public class Facet {
	
	private String type;
	
	private String count;
	
	private String title;
	
	private String query;
	
	public Facet() {		
	}
	
	public Facet(String type, String title, String count, String query) {
		this.type = type;
		this.title = title;
		this.count = count;
		this.query = query;		
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	
	
	

}
