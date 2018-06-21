package edu.harvard.dbmi.avillach.service;

import edu.harvard.dbmi.avillach.data.entity.Resource;
import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.util.response.PICSUREResponse;
import edu.harvard.dbmi.avillach.utils.PicsureWarNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service handling business logic for CRUD on resources
 */
@Path("/resource")
public class PicsureResourceService extends PicsureBaseEntityService<Resource>{

    Logger logger = LoggerFactory.getLogger(PicsureResourceService.class);

    @Inject
    ResourceRepository resourceRepo;

    public PicsureResourceService() {
        super(Resource.class);
    }

    @GET
    @RolesAllowed(PicsureWarNaming.RoleNaming.ROLE_SYSTEM)
    @Path("/{resourceId}")
    public Response getEntityById(
            @PathParam("resourceId") String resourceId) {
        return getEntityById(resourceId, resourceRepo);
    }

    @GET
    @RolesAllowed(PicsureWarNaming.RoleNaming.ROLE_SYSTEM)
    @Path("")
    public Response getResourceAll() {
        logger.info("Getting all resources...");
        List<Resource> resources = null;

        resources = resourceRepo.list();

        if (resources == null)
            return PICSUREResponse.applicationError("Error occurs when listing all resources.");

        return PICSUREResponse.success(resources);
    }

    @POST
    @RolesAllowed(PicsureWarNaming.RoleNaming.ROLE_SYSTEM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response addResource(List<Resource> resources){
        return addEntity(resources, resourceRepo);
    }

    @PUT
    @RolesAllowed(PicsureWarNaming.RoleNaming.ROLE_SYSTEM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response updateResource(List<Resource> resources){
        return updateEntity(resources, resourceRepo);
    }

    @Transactional
    @DELETE
    @RolesAllowed(PicsureWarNaming.RoleNaming.ROLE_SYSTEM)
    @Path("/{resourceId}")
    public Response removeEntityById(@PathParam("resourceId") final String resourceId) {
        return removeEntityById(resourceId, resourceRepo);
    }

}
