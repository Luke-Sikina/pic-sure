package edu.harvard.dbmi.avillach.data.entity;

import javax.persistence.Entity;

@Entity
public class Resource extends BaseEntity{

	private String name;
	private String description;
	private String baseUrl;
	
	public String getName() {
		return name;
	}
	public Resource setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	public Resource setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	public Resource setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}
}
