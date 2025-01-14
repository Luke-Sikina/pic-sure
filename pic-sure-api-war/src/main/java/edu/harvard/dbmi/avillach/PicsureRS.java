package edu.harvard.dbmi.avillach;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.dbmi.avillach.service.PicsureInfoService;
import edu.harvard.dbmi.avillach.service.PicsureQueryService;
import edu.harvard.dbmi.avillach.service.PicsureSearchService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@OpenAPIDefinition(info = @Info(title = "Pic-sure API", version = "1.0.0", description = "This is the Pic-sure API."))
@Path("/")
@Produces("application/json")
@Consumes("application/json")
public class PicsureRS {
	
	@Inject
	PicsureInfoService infoService;
	
	@Inject
	PicsureSearchService searchService;
	
	@Inject
	PicsureQueryService queryService;

	@POST
	@Path("/info/{resourceId}")
	@Operation(
			summary = "Returns information about the provided resource",
			tags = { "info" },
			operationId = "resourceInfo",
			responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Resource information",
					content = @io.swagger.v3.oas.annotations.media.Content(
							schema = @io.swagger.v3.oas.annotations.media.Schema(
									implementation = ResourceInfo.class
							)
					)
			)}
	)
	public ResourceInfo resourceInfo(@Parameter(description="The UUID of the resource to fetch information about") @PathParam("resourceId") String resourceId,
									 @Parameter QueryRequest credentialsQueryRequest,
									 @Context HttpHeaders headers) {
		System.out.println("Resource info requested for : " + resourceId);
		return infoService.info(UUID.fromString(resourceId), credentialsQueryRequest, headers);
	}
	
	@GET
	@Path("/info/resources")
	@Operation(
			summary = "Returns list of resources available",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Resource information",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = Map.class
									)
							)
					)
			}
	)
	public Map<UUID,String> resources(@Context HttpHeaders headers){
		return infoService.resources(headers);
	}

	@GET
	@Path("/search/{resourceId}/values/")
	@Consumes("*/*")
	public PaginatedSearchResult<?> searchGenomicConceptValues(
			@PathParam("resourceId") UUID resourceId,
			QueryRequest searchQueryRequest,
			@QueryParam("genomicConceptPath") String genomicConceptPath,
			@QueryParam("query") String query,
			@QueryParam("page") Integer page,
			@QueryParam("size") Integer size,
			@Context HttpHeaders headers
	) {
		return searchService.searchGenomicConceptValues(resourceId, searchQueryRequest, genomicConceptPath, query, page, size, headers);
	}
	
	@POST
	@Path("/search/{resourceId}")
	@Operation(
		summary = "Searches for concept paths on the given resource matching the supplied search term",
		responses = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "200",
				description = "Search results",
				content = @io.swagger.v3.oas.annotations.media.Content(
						schema = @io.swagger.v3.oas.annotations.media.Schema(
								implementation = SearchResults.class
						)
				)
		)},
		requestBody = @RequestBody(
				required = true,
				content = @io.swagger.v3.oas.annotations.media.Content(
						schema = @io.swagger.v3.oas.annotations.media.Schema(
								example = "{ \"query\": \"searchTerm\" }"
						)
				)
		)
	)
	public SearchResults search(@Parameter(description="The UUID of the resource to search") @PathParam("resourceId") UUID resourceId,
								@Parameter(hidden = true) QueryRequest searchQueryRequest,
								@Context HttpHeaders headers) {
		return searchService.search(resourceId, searchQueryRequest, headers);
	}

	@POST
	@Path("/query")
	@Operation(
			summary = "Submits a query to the given resource",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Query status",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = QueryStatus.class
									)
							)
					)
			}
	)
	public QueryStatus query(@Parameter QueryRequest dataQueryRequest, @Context HttpHeaders headers) {
		return queryService.query(dataQueryRequest, headers);
	}
	
	@POST
	@Path("/query/{queryId}/status")
	@Operation(
			summary = "Returns the status of the given query",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Query status",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = QueryStatus.class
									)
							)
					)
			}
	)
	public QueryStatus queryStatus(@Parameter(description="The UUID of the query to fetch the status of. The UUID is " +
			"returned by the /query endpoint as the \"picsureResultId\" in the response object") @PathParam("queryId") UUID queryId,
								   @Parameter QueryRequest credentialsQueryRequest, @Context HttpHeaders headers) {
		return queryService.queryStatus(queryId, credentialsQueryRequest, headers);
	}
	
	@POST
	@Path("/query/{queryId}/result")
	@Operation(
			summary = "Returns result for given query",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Query result",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = Response.class
									)
							)
					)
			}
	)
	public Response queryResult(@Parameter(description="The UUID of the query to fetch the status of. The UUID is " +
			"returned by the /query endpoint as the \"picsureResultId\" in the response object") @PathParam("queryId") UUID queryId,
								@Parameter QueryRequest credentialsQueryRequest,
								@Context HttpHeaders headers) {
		return queryService.queryResult(queryId, credentialsQueryRequest, headers);
	}

	@POST
	@Path("/query/sync")
	@Operation(
			summary = "Returns result for given query",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Query result",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = Response.class
									)
							)
					)
			}
	)
	public Response querySync(@Context HttpHeaders headers,
			@Parameter(description="Object with field named 'resourceCredentials' which is a key-value map, " +
										"key is identifier for resource, value is token for resource") QueryRequest credentialsQueryRequest) {
		return queryService.querySync(credentialsQueryRequest, headers);
	}
	
	@GET
	@Path("/query/{queryId}/metadata")
	@Operation(
			summary = "Returns metadata for given query",
			description = "Generally used to reconstruct a query that was previously submitted.	The queryId is " +
					"returned by the /query endpoint as the \"picsureResultId\" in the response object",
			responses = {
					@io.swagger.v3.oas.annotations.responses.ApiResponse(
							responseCode = "200",
							description = "Query metadata",
							content = @io.swagger.v3.oas.annotations.media.Content(
									schema = @io.swagger.v3.oas.annotations.media.Schema(
											implementation = QueryStatus.class
									)
							)
					)
			}
	)
	public QueryStatus queryMetadata(@PathParam("queryId") UUID queryId, @Context HttpHeaders headers){
		return queryService.queryMetadata(queryId, headers);
	}
	
}
