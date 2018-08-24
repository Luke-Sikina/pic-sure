package edu.harvard.dbmi.avillach;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import edu.harvard.dbmi.avillach.domain.QueryRequest;
import edu.harvard.dbmi.avillach.util.PicSureStatus;
import edu.harvard.hms.dbmi.avillach.GnomeI2B2CountResourceRS;
import edu.harvard.hms.dbmi.avillach.IRCTResourceRS;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.*;

import static edu.harvard.dbmi.avillach.service.HttpClientUtil.retrieveGetResponse;
import static edu.harvard.dbmi.avillach.service.HttpClientUtil.retrievePostResponse;
import static edu.harvard.dbmi.avillach.service.HttpClientUtil.composeURL;
import static org.junit.Assert.*;

//Need tests executed in order to fill in variables for later tests
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GnomeI2B2ResourceIT extends BaseIT {

    private final static String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb29AYmFyLmNvbSIsImlzcyI6ImJhciIsImV4cCI6MTU0NDQ0MjM0NCwiaWF0IjoxNTM0NDQyMzQ0LCJqdGkiOiJGb28iLCJlbWFpbCI6ImZvb0BiYXIuY29tIn0.u4VVxoGVrxvb8s6QWALqs_BRZnwZ5BX58ZVJS1v6Yls";
    private final static String i2b2queryString = "{" +
            "    \"select\": [" +
            "        {" +
            "            \"alias\": \"gender\", \"field\": {\"pui\": \"/i2b2-wildfly-grin-patient-mapping/Demo/GRIN/GRIN/DEMOGRAPHIC/SEX/M\", \"dataType\":\"STRING\"}" +
            "        }," +
            "        {" +
            "            \"alias\": \"gender\", \"field\": {\"pui\": \"/i2b2-wildfly-grin-patient-mapping/Demo/GRIN/GRIN/DEMOGRAPHIC/SEX/F\", \"dataType\":\"STRING\"}" +
            "        }," +
            "        {" +
            "            \"alias\": \"age\",    \"field\": {\"pui\": \"/i2b2-wildfly-grin-patient-mapping/Demo/GRIN/GRIN/DEMOGRAPHIC/Age\", \"dataType\":\"STRING\"}" +
            "        }" +
            "    ]," +
            "    \"where\": [" +
            "        {" +
            "            \"predicate\": \"CONTAINS\"," +
            "            \"field\": {" +
            "                \"pui\": \"/i2b2-wildfly-grin-patient-mapping/Demo/GRIN/GRIN/DEMOGRAPHIC/SEX/M\"," +
            "                \"dataType\": \"STRING\"" +
            "            }," +
            "            \"fields\": {" +
            "                \"ENOUNTER\": \"YES\"" +
            "            }" +
            "        }" +
            "    ]" +
            "}";
    private final static String gnomeQueryString = "{" +
            " \"where\": [" +
            "   { \"field\" : " +
            "       {" +
            "           \"pui\" : \"/gnome/query_rest.cgi\"," +
            "           \"dataType\": \"STRING\"" +
            "       }," +
            "   \"predicate\" : \"CONTAINS\"," +
            "       \"fields\" : {" +
            "           \"qtype\" : \"variants\"," +
            "           \"vqueries\" : [\"chr16,2120487,2120487,G,A\"]" +
            "       }" +
            "   }]} ";
    private final static String errorQueryString = "{" +
            "    \"where\": [" +
            "        {" +
            "            \"predicate\": \"CONTAINS\"," +
            "            \"field\": {" +
            "                \"pui\": \"/i2b2-wildfly-grin-patient-mapping/Demo/GRIN/GRIN/DEMOGRAPHIC/nonexistentpath\"," +
            "                \"dataType\": \"STRING\"" +
            "            }," +
            "            \"fields\": {" +
            "                \"ENOUNTER\": \"YES\"" +
            "            }" +
            "        }" +
            "    ]" +
            "}";

    private static JsonNode i2b2Query;
    private static JsonNode gnomeQuery;
    private static JsonNode errorQuery;

    private static UUID gnomeI2B2UUID;
    private static Header[] headers = new Header[1];
    private static String queryId;
    private static String errorQueryId;
    private static String status;

    @BeforeClass
    public static void setUp() throws IOException{
        //Will need to know the resource uuid
        String jwt = generateJwtForSystemUser();
        headers[0] = new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        HttpResponse response = retrieveGetResponse(endpointUrl+"/info/resources", headers);
        assertEquals("Response status code should be 200", 200, response.getStatusLine().getStatusCode());
        List<JsonNode> responseBody = json.readValue(response.getEntity().getContent(), new TypeReference<List<JsonNode>>(){});
        assertFalse(responseBody.isEmpty());

        for (JsonNode node : responseBody){
            if (node.get("name").asText().equals("Gnome I2B2 Count Resource RS")){
                gnomeI2B2UUID = UUID.fromString(node.get("uuid").asText());
            }
        }

        i2b2Query = json.readTree(i2b2queryString);
        gnomeQuery = json.readTree(gnomeQueryString);
        errorQuery = json.readTree(errorQueryString);
    }

    @Test
    public void testQuery() throws IOException {
        //Create a query
        QueryRequest queryRequest = new QueryRequest();
        Map<String, String> credentials = new HashMap<String, String>();
        Map<String, Object> queryMap = new HashMap<>();
        queryRequest.setResourceCredentials(credentials);
        queryMap.put("i2b2", i2b2Query);
        queryMap.put("gnome", gnomeQuery);
        queryRequest.setQuery(queryMap);
        queryRequest.setResourceUUID(gnomeI2B2UUID);

        String body = json.writeValueAsString(queryRequest);

        //Should throw an error if credentials missing or wrong
        HttpResponse response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        JsonNode responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        String errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        String errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        credentials.put(GnomeI2B2CountResourceRS.GNOME_BEARER_TOKEN_KEY, "anInvalidToken");
        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, token);
        queryRequest.setResourceCredentials(credentials);
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        //Should throw an error if missing any query
        credentials.put(GnomeI2B2CountResourceRS.GNOME_BEARER_TOKEN_KEY, token);
        queryRequest.setResourceCredentials(credentials);
        queryRequest.setQuery(null);
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Missing query should return a 500", 500, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);

        queryMap.remove("gnome");
        queryRequest.setQuery(queryMap);
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Missing query should return a 500", 500, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);


        //Try a poorly worded queryString
        queryMap.put("gnome", "poorly worded query");
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Incorrectly formatted query should return a 500", 500, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);

        //Make sure all queries work
        queryMap.put("gnome", gnomeQuery);
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        assertNotNull("Status should not be null", responseMessage.get("status"));
        queryId = responseMessage.get("picsureResultId").asText();
        assertNotNull("picsureResultId should not be null", queryId);

        //Want the status to be ERROR if one query errors - send query to be tested by queryStatus
        queryMap.put("i2b2", errorQuery);
        body = json.writeValueAsString(queryRequest);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"), headers, body);
        assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        errorQueryId = responseMessage.get("picsureResultId").asText();
        assertNotNull("Status should not be null", responseMessage.get("status"));

    }

    @Test
    public void testQueryStatus() throws IOException {
        Map<String, String> credentials = new HashMap<String, String>();
        String body = json.writeValueAsString(credentials);

        //Should get 401 for missing or invalid credentials
        HttpResponse response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/status"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        JsonNode responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        String errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        String errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, "anInvalidToken");
        credentials.put(GnomeI2B2CountResourceRS.GNOME_BEARER_TOKEN_KEY, token);
        body = json.writeValueAsString(credentials);

        response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/status"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        //This should retrieve the status of the query successfully
        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, token);
        body = json.writeValueAsString(credentials);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/status"), headers, body);
        assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        status = responseMessage.get("status").asText();
        assertNotNull("Status should not be null", status);

        //This query should eventually result in an error, since one of the queries should have errored
        String errorStatus = PicSureStatus.PENDING.name();
        while (errorStatus.equals(PicSureStatus.PENDING.name())){
            response = retrievePostResponse(composeURL(endpointUrl,"/query/"+errorQueryId+"/status"), headers, body);
            assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
            responseMessage = json.readTree(response.getEntity().getContent());
            assertNotNull("Response message should not be null", responseMessage);
            errorStatus = responseMessage.get("status").asText();
        }
        assertEquals("Status should be ERROR", PicSureStatus.ERROR.name(), errorStatus);


    }

    @Test
    public void testResult() throws IOException, InterruptedException {
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, token);
        credentials.put(GnomeI2B2CountResourceRS.GNOME_BEARER_TOKEN_KEY, token);
        String body = json.writeValueAsString(credentials);

        //Need to make sure result is ready
        while (!status.equals(PicSureStatus.AVAILABLE.name())){
            Thread.sleep(2000);
            HttpResponse response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/status"), headers, body);
            assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
            JsonNode responseMessage = json.readTree(response.getEntity().getContent());
            assertNotNull("Response message should not be null", responseMessage);
            status = responseMessage.get("status").asText();
        }

        credentials.remove(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY);
        body = json.writeValueAsString(credentials);

        //Missing or invalid credentials should return 401
        HttpResponse response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/result"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        JsonNode responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        String errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        String errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, "anInvalidToken");
        body = json.writeValueAsString(credentials);

        response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/result"), headers, body);
        assertEquals("Missing credentials should return a 401", 401, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        errorType = responseMessage.get("errorType").asText();
        assertEquals("Error type should be error", "error", errorType);
        errorMessage = responseMessage.get("message").asText();
        assertTrue("Error message should be Unauthorized", errorMessage.contains("Unauthorized"));

        //Should return an array of results
        credentials.put(GnomeI2B2CountResourceRS.I2B2_BEARER_TOKEN_KEY, token);
        body = json.writeValueAsString(credentials);
        response = retrievePostResponse(composeURL(endpointUrl,"/query/"+queryId+"/result"), headers, body);
        assertEquals("Should return a 200", 200, response.getStatusLine().getStatusCode());
        responseMessage = json.readTree(response.getEntity().getContent());
        assertNotNull("Response message should not be null", responseMessage);
        //In testing this was the result, but probably the test shouldn't rely on it
        assertEquals("Result should be 2", 2, Integer.parseInt(responseMessage.toString()));

    }

}
