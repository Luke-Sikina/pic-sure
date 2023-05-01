package edu.harvard.dbmi.avillach.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Schema(description = "resourceCredentials should be a map with the key identifying the resource and the value an authorization" +
		" token for the resource.  The query is a string or object that contains a search term or query")
public class QueryRequest {

	@Schema(description = "Map with the key identifying the resource and the value an authorization token for the resource")
	private Map<String, String> resourceCredentials = new HashMap<>();

	@Schema(description = "A string or object that contains a search term or query. Potential Expect Result Type: " +
			"\"COUNT\", \"CROSS_COUNT\", \"INFO_COLUMN_LISTING\", \"OBSERVATION_COUNT\", \"OBSERVATION_CROSS_COUNT\"",
			example = "{\n" +
					"    \"resourceUUID\": \"de273216-3b46-4dec-a330-1aa0634624ae\",\n" +
					"    \"query\": {\n" +
					"        \"categoryFilters\": {\n" +
					"            \"\\\\ACT Demographics\\\\Sex\\\\\": [\n" +
					"                \"Female\",\n" +
					"                \"Male\"\n" +
					"            ]\n" +
					"        },\n" +
					"        \"numericFilters\": {\n" +
					"            \"\\\\ACT Demographics\\\\Age\\\\\": {\n" +
					"                \"min\": \"5\",\n" +
					"                \"max\": \"12\"\n" +
					"            }\n" +
					"        },\n" +
					"        \"requiredFields\": [],\n" +
					"        \"anyRecordOf\": [],\n" +
					"        \"variantInfoFilters\": [\n" +
					"            {\n" +
					"                \"categoryVariantInfoFilters\": {\n" +
					"                    \"Gene_with_variant\": [\n" +
					"                        \"CHD8\"\n" +
					"                    ]\n" +
					"                },\n" +
					"                \"numericVariantInfoFilters\": {}\n" +
					"            }\n" +
					"        ],\n" +
					"        \"expectedResultType\": \"COUNT\"\n" +
					"    }\n" +
					"}")
	private Object query;

	@Schema(description = "The UUID of the resource to query")
	private UUID resourceUUID;

	public Map<String, String> getResourceCredentials() {
		return resourceCredentials;
	}
	public QueryRequest setResourceCredentials(Map<String, String> resourceCredentials) {
		this.resourceCredentials = resourceCredentials;
		return this;
	}

	public Object getQuery() {
		return query;
	}
	public QueryRequest setQuery(Object query) {
		this.query = query;
		return this;
	}

	public UUID getResourceUUID() {
		return resourceUUID;
	}

	public void setResourceUUID(UUID resourceUUID) {
		this.resourceUUID = resourceUUID;
	}
}
