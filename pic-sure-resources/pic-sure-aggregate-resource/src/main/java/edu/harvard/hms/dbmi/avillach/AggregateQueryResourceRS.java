package edu.harvard.hms.dbmi.avillach;

import java.io.IOException;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.dbmi.avillach.service.IResourceRS;
import edu.harvard.dbmi.avillach.util.exception.ApplicationException;
import edu.harvard.dbmi.avillach.util.exception.ProtocolException;
import edu.harvard.dbmi.avillach.util.exception.ResourceInterfaceException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.harvard.dbmi.avillach.service.HttpClientUtil.*;


@Path("/group")
@Produces("application/json")
@Consumes("application/json")
public class AggregateQueryResourceRS implements IResourceRS
{
	private static final String TARGET_PICSURE_URL = System.getenv("TARGET_PICSURE_URL");
	private static final String PICSURE_2_TOKEN = System.getenv("PICSURE_2_TOKEN");

	private static final String BEARER_STRING = "Bearer ";
	public static final String MISSING_REQUEST_DATA_MESSAGE = "Missing query request data";
	public static final String MISSING_CREDENTIALS_MESSAGE = "Missing credentials for resource with id ";
	public static final String INCORRECTLY_FORMATTED_REQUEST = "Incorrectly formatted query request data";

	private Header[] headers = new Header[1];

	private final static ObjectMapper json = new ObjectMapper();
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	public AggregateQueryResourceRS() {
		if(TARGET_PICSURE_URL == null)
			throw new RuntimeException("TARGET_PICSURE_URL environment variable must be set.");
		if(PICSURE_2_TOKEN == null)
			throw new RuntimeException("PICSURE_2_TOKEN environment variable must be set.");
		Header authorizationHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, BEARER_STRING + PICSURE_2_TOKEN);
		headers[0] = authorizationHeader;
	}
	
	@GET
	@Path("/status")
	public Response status() {
		return Response.ok().build();
	}

	@GET
	@Path("/info")
	public ResourceInfo info(Map<String, String> resourceCredentials){
		return new ResourceInfo();
	}

	@POST
	@Path("/search")
	public SearchResults search(QueryRequest searchJson){
		return new SearchResults();
	}


	@POST
	@Path("/query")
	public QueryStatus query(QueryRequest queryRequest) {
		logger.debug("Calling AggregateQueryResource query()");
		if (queryRequest == null) {
			throw new ProtocolException(MISSING_REQUEST_DATA_MESSAGE);
		}
		QueryStatus statusResponse = new QueryStatus();
		statusResponse.setStartTime(new Date().getTime());
		Set<PicSureStatus> presentStatuses = new HashSet<>();
		ArrayList<UUID> queryIdList = new ArrayList<>();
		try {
			List<Object> requests = (List<Object>) queryRequest.getQuery();
			for (Object o : requests) {
				QueryRequest qr = json.convertValue(o, QueryRequest.class);

				Map<String, String> resourceCredentials = qr.getResourceCredentials();
				if (resourceCredentials == null) {
					throw new NotAuthorizedException(MISSING_CREDENTIALS_MESSAGE + qr.getResourceUUID());
				}
				try {
					String queryString = json.writeValueAsString(qr);
					String pathName = "/query/" + qr.getResourceUUID();
					HttpResponse response = retrievePostResponse(TARGET_PICSURE_URL + pathName, headers, queryString);
					if (response.getStatusLine().getStatusCode() != 200) {
						logger.error(TARGET_PICSURE_URL + " calling resource with id " + qr.getResourceUUID() + " did not return a 200: {} {} ", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
						if (response.getStatusLine().getStatusCode() == 401) {
							throw new NotAuthorizedException(TARGET_PICSURE_URL + " calling resource with id " + qr.getResourceUUID() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
						}
						throw new ResourceInterfaceException(TARGET_PICSURE_URL + " calling resource with id " + qr.getResourceUUID() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
					}
					QueryStatus status = readObjectFromResponse(response, QueryStatus.class);
					//TODO What other information do we need to keep from this?
					presentStatuses.add(status.getStatus());
					queryIdList.add(status.getPicsureResultId());
				} catch (IOException e) {
					throw new ApplicationException("Error encoding query for resource with id " + qr.getResourceUUID());
				}
			}
		} catch (ClassCastException | IllegalArgumentException e){
			logger.error(e.getMessage());
			throw new ProtocolException(INCORRECTLY_FORMATTED_REQUEST);
		}
        statusResponse.setStatus(determineStatus(presentStatuses));
        statusResponse.setResultMetadata(SerializationUtils.serialize(queryIdList));
		return statusResponse;
	}

	@POST
	@Path("/query/{resourceQueryId}/status")
	public QueryStatus queryStatus(@PathParam("resourceQueryId")String queryId, Map<String, String> resourceCredentials) {
		logger.debug("calling Aggregate Query Resource queryStatus()");
		QueryStatus statusResponse = new QueryStatus();
		statusResponse.setPicsureResultId(UUID.fromString(queryId));
		if (resourceCredentials == null) {
			throw new NotAuthorizedException(MISSING_CREDENTIALS_MESSAGE);
		}

		String pathName = "/query/" + queryId + "/metadata";
		HttpResponse response = retrieveGetResponse(TARGET_PICSURE_URL + pathName, headers);
		QueryStatus status = readObjectFromResponse(response, QueryStatus.class);
		try {
			ArrayList<UUID> queryIdList = SerializationUtils.deserialize(status.getResultMetadata());
			Set<PicSureStatus> presentStatuses = new HashSet<>();

			for (UUID qid : queryIdList) {
				pathName = "/query/" + qid + "/status";
				try {
					String body = json.writeValueAsString(resourceCredentials);

					response = retrievePostResponse(TARGET_PICSURE_URL + pathName, headers, body);
					if (response.getStatusLine().getStatusCode() != 200) {
						logger.error(TARGET_PICSURE_URL + " did not return a 200: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
						if (response.getStatusLine().getStatusCode() == 401) {
							throw new NotAuthorizedException(TARGET_PICSURE_URL + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
						}
						throw new ResourceInterfaceException(TARGET_PICSURE_URL + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
					}
					status = readObjectFromResponse(response, QueryStatus.class);

					presentStatuses.add(status.getStatus());
				} catch (IOException e) {
					throw new ApplicationException("Unable to encode resource credentials");
				}
			}
			statusResponse.setStatus(determineStatus(presentStatuses));
			return statusResponse;
		} catch (IllegalArgumentException e){
			throw new ApplicationException("Unable to fetch subqueries");
		}
	}

	@POST
	@Path("/query/{resourceQueryId}/result")
	public Response queryResult(@PathParam("resourceQueryId") String queryId, Map<String, String> resourceCredentials) {
		logger.debug("calling Aggregate Query Resource queryResult()");
		if (resourceCredentials == null) {
			throw new NotAuthorizedException(MISSING_CREDENTIALS_MESSAGE);
		}

		String pathName = "/query/" + queryId + "/metadata";
		HttpResponse response = retrieveGetResponse(TARGET_PICSURE_URL + pathName, headers);
		QueryStatus status = readObjectFromResponse(response, QueryStatus.class);
		try {
			ArrayList<UUID> queryIdList = SerializationUtils.deserialize(status.getResultMetadata());

			//TODO What format to actually put this in
			List<JsonNode> responses = new ArrayList<>();
			for (UUID qid : queryIdList) {
				pathName = "/query/" + qid + "/result";
				try {
					String body = json.writeValueAsString(resourceCredentials);

					response = retrievePostResponse(TARGET_PICSURE_URL + pathName, headers, body);
					if (response.getStatusLine().getStatusCode() != 200) {
						logger.error(TARGET_PICSURE_URL + " did not return a 200: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
						if (response.getStatusLine().getStatusCode() == 401) {
							throw new NotAuthorizedException(TARGET_PICSURE_URL + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
						}
						throw new ResourceInterfaceException(TARGET_PICSURE_URL + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
					}
					//TODO Probably not this
					responses.add(json.readTree(response.getEntity().getContent()));

				} catch (IOException e) {
					throw new ApplicationException("Unable to encode resource credentials");
				}
			}
			return Response.ok(responses).build();
		} catch (IllegalArgumentException e){
			throw new ApplicationException("Unable to fetch subqueries");
		}
	}

	private PicSureStatus determineStatus(Set statuses){
		if (statuses.contains(PicSureStatus.ERROR)) {
			return PicSureStatus.ERROR;
		} else if (statuses.contains(PicSureStatus.PENDING) || statuses.contains(PicSureStatus.QUEUED)) {
			return PicSureStatus.PENDING;
		} else if (statuses.contains(PicSureStatus.AVAILABLE)) {
			return PicSureStatus.AVAILABLE;
		}
		return null;
	}
}
