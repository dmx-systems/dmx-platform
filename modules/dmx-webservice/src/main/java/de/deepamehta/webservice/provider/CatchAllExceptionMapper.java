package de.deepamehta.webservice.provider;

import de.deepamehta.core.util.UniversalExceptionMapper;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



/**
 * This mapper maps <i>all</i> Throwables to a suitable response.
 * <p>
 * We don't want Jersey to re-throw anything to the HTTP container as this would result in logging
 * the exception twice and possibly in interspersed illegible stack traces (see #484).
 * <p>
 * 2 additional aspects are handled:
 *   - Logging the exception.
 *   - Enriching the response with an error entity.
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Throwable> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    HttpServletRequest request;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Response toResponse(Throwable e) {
        return new UniversalExceptionMapper(e, request).toResponse();
    }
}
