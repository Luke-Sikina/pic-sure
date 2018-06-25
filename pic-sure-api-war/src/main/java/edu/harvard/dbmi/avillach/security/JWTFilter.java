package edu.harvard.dbmi.avillach.security;

import java.io.IOException;
import java.util.Base64;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import edu.harvard.dbmi.avillach.data.entity.User;
import edu.harvard.dbmi.avillach.data.repository.UserRepository;
import edu.harvard.dbmi.avillach.util.response.PICSUREResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class JWTFilter implements ContainerRequestFilter {

	Logger logger = LoggerFactory.getLogger(JWTFilter.class);

	@Context
	ResourceInfo resourceInfo;
	
	@Resource(mappedName = "java:global/client_secret")
	private String clientSecret;
	@Resource(mappedName = "java:global/user_id_claim")
	private String userIdClaim;
	
	@Inject
	UserRepository userRepo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		logger.debug("Entered jwtfilter.filter()...");
		String tokenForLogging = null;
		User userForLogging = null;
		try {
			String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

			if (authorizationHeader == null || authorizationHeader.isEmpty()) {
				requestContext.abortWith(PICSUREResponse.protocolError("No authorization header found."));
				return;
			}

			String token = authorizationHeader.substring(6).trim();
			tokenForLogging = token;

			Jws<Claims> jws = Jwts.parser().setSigningKey(clientSecret.getBytes()).parseClaimsJws(token);

			String subject = jws.getBody().getSubject();
			
			String userId = jws.getBody().get(userIdClaim, String.class);
						
			User authenticatedUser = userRepo.findOrCreate(subject, userId);

			if (authenticatedUser == null) {
				logger.error("Cannot find or create a user from token: " + token);
				requestContext.abortWith(PICSUREResponse.unauthorizedError("Cannot find or create a user"));
				return;
			}

			userForLogging = authenticatedUser;

			String[] rolesAllowed = resourceInfo.getResourceMethod().isAnnotationPresent(RolesAllowed.class)
					? resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class).value()
							: new String[]{};
			for(String role : rolesAllowed) {
				if(authenticatedUser.getRoles() == null
					|| !authenticatedUser.getRoles().contains(role)) {
					logger.error("The roles of the user - " + userForLogging + " - doesn't match the restrictions.");
					requestContext.abortWith(PICSUREResponse.unauthorizedError("User has insufficient privileges."));
					return;
				}
			}

			logger.info("User - " + userForLogging + " - has just passed all the jwtfilter.filter() layer.");

		} catch (SignatureException e) {
			logger.error("The signature of token - " + tokenForLogging + " - is invalid.");
			requestContext.abortWith(PICSUREResponse.unauthorizedError("Token is invalid."));
		} catch (NotAuthorizedException e) {
			logger.error("User - " + userForLogging + " - has insufficient privileges.");
			// we should show different response based on role
			requestContext.abortWith(PICSUREResponse.unauthorizedError("User has insufficient privileges."));
		} catch (Exception e){
			// we should show different response based on role
			e.printStackTrace();
			requestContext.abortWith(PICSUREResponse.applicationError("Inner application error, please contact system admin"));
		}
	}

}
