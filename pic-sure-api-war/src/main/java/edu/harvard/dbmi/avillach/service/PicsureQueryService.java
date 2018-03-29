package edu.harvard.dbmi.avillach.service;

import java.sql.Date;
import java.util.Map;
import java.util.UUID;

import edu.harvard.dbmi.avillach.data.entity.Query;
import edu.harvard.dbmi.avillach.data.entity.Resource;
import edu.harvard.dbmi.avillach.data.repository.QueryRepository;
import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.domain.QueryRequest;
import edu.harvard.dbmi.avillach.domain.QueryResults;
import edu.harvard.dbmi.avillach.domain.QueryStatus;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * Service handling business logic for queries to resources
 */
public class PicsureQueryService {

	@Inject
	ResourceRepository resourceRepo;

	@Inject
	QueryRepository queryRepo;

	@Inject
	ResourceWebClient resourceWebClient;

	@PersistenceContext
	private EntityManager em;

	/**
	 * Executes a query on a PIC-SURE resource and creates a QueryResults object in the
	 * database for the query.
	 * 
	 * @param resourceId - id of targeted resource
	 * @param dataQueryRequest - - {@link QueryRequest} containing resource specific credentials object
	 *                       and resource specific query (could be a string or a json o
	 * @return {@link QueryResults} object
	 */
	@Transactional
	public QueryResults query(UUID resourceId, QueryRequest dataQueryRequest) {
		Resource resource = resourceRepo.getById(resourceId);
		if (resource == null){
			//TODO Create custom exception
			throw new RuntimeException("No resource with id " + resourceId.toString() + " exists");
		}
		QueryResults results = resourceWebClient.query(resource.getBaseUrl(), dataQueryRequest);
		//TODO Deal with possible errors
        //Save query entity
		Query queryEntity = new Query();
		queryEntity.setResourceResultId(results.getResourceResultId());
		queryEntity.setResource(resource);
		queryEntity.setStatus(results.getStatus().getStatus());
		queryEntity.setStartTime(new Date(results.getStatus().getStartTime()));
		queryEntity.setQuery(dataQueryRequest.getQuery().toString());
		em.persist(queryEntity);
		results.setPicsureResultId(queryEntity.getUuid());
		results.getStatus().setResourceID(resourceId);
		return results;
	}

	/**
	 * Retrieves the {@link QueryStatus} for a given queryId by looking up the target resource 
	 * from the database and calling the target resource for an updated status. The QueryResults
	 * in the database are updated each time this is called.
	 * 
	 * @param queryId - id of targeted resource
	 * @param resourceCredentials - resource specific credentials object
	 * @return {@link QueryStatus}
	 */
	@Transactional
	public QueryStatus queryStatus(UUID queryId, Map<String, String> resourceCredentials) {
		Query query = queryRepo.getById(queryId);
		if (query == null){
			//TODO Create custom exception
			throw new RuntimeException("No query with id " + queryId.toString() + " exists");
		}
		Resource resource = query.getResource();
		//Update status on query object
		QueryStatus status = resourceWebClient.queryStatus(resource.getBaseUrl(), query.getResourceResultId(), resourceCredentials);
		query.setStatus(status.getStatus());
		em.persist(query);
		status.setStartTime(query.getStartTime().getTime());
		status.setResourceID(resource.getUuid());
		return status;
	}

	/**
	 * Streams the result for a given queryId by looking up the target resource
	 * from the database and calling the target resource for a result. The queryStatus
	 * method should be used to verify that the result is available prior to retrieving it.
	 * 
	 * @param queryId - id of target resource
	 * @param resourceCredentials - resource specific credentials object
	 * @return {@link QueryResults}
	 */
	@Transactional
	public QueryResults queryResult(UUID queryId, Map<String, String> resourceCredentials) {
		Query query = queryRepo.getById(queryId);
		if (query == null){
			//TODO Create custom exception
			throw new RuntimeException("No query with id " + queryId.toString() + " exists");
		}
		Resource resource = query.getResource();
		//TODO Need to pass info from original query somehow?
		QueryResults results = resourceWebClient.queryResult(resource.getBaseUrl(), query.getResourceResultId(), resourceCredentials);
		//Update query object
		results.setPicsureResultId(queryId);
		results.getStatus().setResourceID(resource.getUuid());
		query.setStatus(results.getStatus().getStatus());
		em.persist(query);
		return results;
	}

}
