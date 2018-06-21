package edu.harvard.dbmi.avillach.data.entity;

import edu.harvard.dbmi.avillach.util.PicSureStatus;

import javax.persistence.*;
import java.sql.Date;

@Entity(name = "query")
public class Query extends BaseEntity {

	//TODO may not need these two things
	private Date startTime;
	
	private Date readyTime;

	//Resource is responsible for mapping internal status to picsurestatus
	private PicSureStatus status;

	private String resourceResultId;

	//Original query request
	@Column(length = 8192)
	private String query;

	@ManyToOne
	@JoinColumn(name = "resourceId")
	private Resource resource;

	@Column(length = 8192)
	private byte[] metadata;

	public Resource getResource() {
		return resource;
	}

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getResourceResultId() {
		return resourceResultId;
	}

	public void setResourceResultId(String resourceResultId) {
		this.resourceResultId = resourceResultId;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getReadyTime() {
		return readyTime;
	}

	public PicSureStatus getStatus() {
		return status;
	}

	public void setReadyTime(Date readyTime) {
		this.readyTime = readyTime;
	}

	public void setStatus(PicSureStatus status) {
		this.status = status;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public byte[] getMetadata() {
		return metadata;
	}

	public void setMetadata(byte[] metadata) {
		this.metadata = metadata;
	}
}
