package edu.harvard.dbmi.avillach;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.dbmi.avillach.data.entity.Resource;
import edu.harvard.hms.dbmi.avillach.IRCTResourceRS;

import static edu.harvard.dbmi.avillach.service.HttpClientUtil.composeURL;

@Singleton
@Startup
public class ResourceTestInitializer
{
    public static String TARGET_PICSURE_URL = System.getenv("TARGET_PICSURE_URL");
    public static String TARGET_IRCT_URL = System.getenv("TARGET_IRCT_URL");
    public static String AGGREGATE_RS_URL = System.getenv("AGGREGATE_RS_URL");
    public static String IRCT_RS_URL = System.getenv("IRCT_RS_URL");
    @PersistenceContext(unitName = "picsure")
    private EntityManager em;

    @PostConstruct
    public void insertTestResources() {
        System.out.println("ResourceTestInitializer, target picsure url is!!!: " + TARGET_PICSURE_URL);

		Resource fooResource = new Resource()
				.setTargetURL(TARGET_IRCT_URL)
//                .setTargetURL("http://localhost:8080/pic-sure-api-wildfly-2.0.0-SNAPSHOT/pic-sure/v1.4")
                .setResourceRSPath(IRCT_RS_URL)
				.setDescription("HMS DBMI NHANES PIC-SURE 1.4  Supply token with key '" + IRCTResourceRS.IRCT_BEARER_TOKEN_KEY + "'")
				.setName("nhanes.hms.harvard.edu")
                .setToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb29AYmFyLmNvbSIsImlzcyI6ImJhciIsImV4cCI6ODY1NTI4Mzk4NTQzLCJpYXQiOjE1Mjg0ODQ5NDMsImp0aSI6IkZvbyIsImVtYWlsIjoiZm9vQGJhci5jb20ifQ.KE2NIfCzQnd_vhykhb0sHdPHEwvy2Wphc4UVsKAVTgM");
		em.persist(fooResource);

        Resource aggregateResource = new Resource()
//                .setTargetURL("http://localhost:8080/pic-sure-api-wildfly-2.0.0-SNAPSHOT/pic-sure/group")
                .setTargetURL(TARGET_PICSURE_URL)
                .setResourceRSPath(AGGREGATE_RS_URL)
                .setToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb29AYmFyLmNvbSIsImlzcyI6ImJhciIsImV4cCI6ODY1NTI4Mzk4NTQzLCJpYXQiOjE1Mjg0ODQ5NDMsImp0aSI6IkZvbyIsImVtYWlsIjoiZm9vQGJhci5jb20ifQ.KE2NIfCzQnd_vhykhb0sHdPHEwvy2Wphc4UVsKAVTgM")
                .setDescription("Aggregate Resource RS")
                .setName("Aggregate Resource RS");
        em.persist(aggregateResource);
    }

}
