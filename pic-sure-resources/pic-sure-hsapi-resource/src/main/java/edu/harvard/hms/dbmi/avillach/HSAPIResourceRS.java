package edu.harvard.hms.dbmi.avillach;

import java.io.IOException;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.dbmi.avillach.util.exception.ProtocolException;
import edu.harvard.dbmi.avillach.util.exception.ResourceInterfaceException;
import org.apache.http.HttpResponse;

import edu.harvard.dbmi.avillach.service.IResourceRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.harvard.dbmi.avillach.service.HttpClientUtil.*;


@Path("/hsapi")
@Produces("application/json")
@Consumes("application/json")
public class HSAPIResourceRS implements IResourceRS
{
	public static final String MISSING_REQUEST_DATA_MESSAGE = "Missing query request data";
	public static final String MISSING_TARGET_URL = "Missing target URL";

	private final static ObjectMapper json = new ObjectMapper();
	private ResourceInfo HSAPIresourceInfo = new ResourceInfo();
	private Logger logger = LoggerFactory.getLogger(this.getClass());


	public HSAPIResourceRS() {
		//This only needs to be done once
		List<QueryFormat> queryFormats = new ArrayList<>();

		// /hsapi/resource/
		QueryFormat resource = new QueryFormat().setName("Resource List").setDescription("List existing resources");
		Map<String, Object> specification = new HashMap<>();
		specification.put("entity", "The type of entity you wish to retrieve or explore - e.g. resource or user");
		specification.put("page", "Optional - A page number within the paginated result set");
		resource.setSpecification(specification);

		Map<String, Object> example = new HashMap<>();
		example.put("entity", "resource");
		List<Map<String, Object>> examples = new ArrayList<>();
		examples.add(example);

		Map<String, Object> example2 = new HashMap<>();
		example2.put("entity", "resource");
		example2.put("page", "2");
		examples.add(example2);

		resource.setExamples(examples);
		queryFormats.add(resource);

		// /hsapi/resource/{id}/files/
		QueryFormat files = new QueryFormat().setName("File List").setDescription("Get a listing of files within a resource");
		specification.put("entity", "The type of entity you wish to retrieve or explore - e.g. resource or user");
		specification.put("id", "The id of the specific entity to retrieve or explore");
		specification.put("subentity", "A type of entity within the main entity - e.g. file (under resource)");
		specification.put("page", "Optional - A page number within the paginated result set");
		files.setSpecification(specification);

		example = new HashMap<>();
		example.put("entity", "resource");
		example.put("id", "a1b23c");
		example.put("subentity", "files");
		examples = new ArrayList<>();
		examples.add(example);

		example2 = new HashMap<>();
		example2.put("entity", "resource");
		example2.put("id", "a1b23c");
		example2.put("subentity", "files");
		example2.put("page", "2");
		examples.add(example2);
		files.setExamples(examples);
		queryFormats.add(files);

		// /hsapi/resource/{id}/files/{pathname}/
		QueryFormat filePath = new QueryFormat().setName("Get File").setDescription("Retrieve a resource file");

		specification.put("entity", "The type of entity you wish to retrieve or explore - e.g. resource or user");
		specification.put("id", "The id of the specific entity to retrieve or explore");
		specification.put("subentity", "A type of entity within the main entity - e.g. file (under resource)");
		specification.put("pathname", "The name or path of the specific subentity you are looking for");
		files.setSpecification(specification);

		example = new HashMap<>();
		example.put("entity", "resource");
		example.put("id", "a1b23c");
		example.put("subentity", "files");
		example.put("pathname", "abc.csv");
		examples = new ArrayList<>();
		examples.add(example);
		filePath.setExamples(examples);
		queryFormats.add(filePath);

		HSAPIresourceInfo.setQueryFormats(queryFormats);
	}

	@GET
	@Path("/status")
	public Response status() {
		return Response.ok().build();
	}

	@POST
	@Path("/info")
	@Override
	public ResourceInfo info(QueryRequest queryRequest) {
		logger.debug("Calling HSAPI Resource info()");
		HSAPIresourceInfo.setName("HSAPI Resource : " + queryRequest.getTargetURL());
		return HSAPIresourceInfo;
	}

	@POST
	@Path("/search")
	@Override
	public SearchResults search(QueryRequest searchJson) {
		logger.debug("Calling HSAPI Resource search()");
		throw new UnsupportedOperationException("Search is not implemented for this resource");
	}

	@POST
	@Path("/query")
	@Override
	public QueryStatus query(QueryRequest queryJson) {
		logger.debug("Calling HSAPI Resource query()");
		throw new UnsupportedOperationException("Query is not implemented in this resource.  Please use query/sync");
	}

	@POST
	@Path("/query/{resourceQueryId}/status")
	@Override
	public QueryStatus queryStatus(@PathParam("resourceQueryId") String queryId, QueryRequest statusQuery) {
		logger.debug("calling HSAPI Resource queryStatus() for query {}", queryId);
		throw new UnsupportedOperationException("Query status is not implemented in this resource.  Please use query/sync");

	}

	@POST
	@Path("/query/{resourceQueryId}/result")
	@Override
	public Response queryResult(@PathParam("resourceQueryId") String queryId, QueryRequest statusQuery) {
		logger.debug("calling HSAPI Resource queryResult() for query {}", queryId);
		throw new UnsupportedOperationException("Query result is not implemented in this resource.  Please use query/sync");
	}

	@POST
	@Path("/query/sync")
	@Override
	public Response querySync(QueryRequest resultRequest) {
		logger.debug("calling HSAPI Resource querySync()");
        if (resultRequest == null){
            throw new ProtocolException(MISSING_REQUEST_DATA_MESSAGE);
        }

        if (resultRequest.getTargetURL() == null){
            throw new ProtocolException(MISSING_TARGET_URL);
        }
		Object queryObject = resultRequest.getQuery();
		if (queryObject == null) {
			throw new ProtocolException((MISSING_REQUEST_DATA_MESSAGE));
		}

		JsonNode queryNode = json.valueToTree(queryObject);

		String path = buildPath(queryNode);

		HttpResponse response = retrieveGetResponse(composeURL(resultRequest.getTargetURL(), path), null);
		if (response.getStatusLine().getStatusCode() != 200) {
			logger.error(resultRequest.getTargetURL() + " did not return a 200: {} {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
			if (response.getStatusLine().getStatusCode() == 401) {
				throw new NotAuthorizedException(resultRequest.getTargetURL() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
			}
				throw new ResourceInterfaceException(resultRequest.getTargetURL() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		}
		try {
			return Response.ok(response.getEntity().getContent()).build();
		} catch (IOException e){
			throw new ResourceInterfaceException("Unable to read the resource response: " + e.getMessage());
		}
	}

	private String buildPath (JsonNode node){
		if (!node.has("entity")){
			throw new ProtocolException("Entity required");
		}
		String path = node.get("entity").asText();

		//We only add each subsequent part if the previous ones are present
		//TODO Should we throw an error if later parts are present without the previous ones,
		// or just ignore it and let the resource return a 404?
		if (node.has("id")){
			path += "/" + node.get("id").asText();
			if (node.has("subentity")){
				path += "/" + node.get("subentity").asText();
				if (node.has("pathname")){
					path += "/" + node.get("pathname").asText();
				}
			}
		}

		if (node.has("page")){
			path += "/?page=" + node.get("page").asText();
		}

		return path;
	}

}
