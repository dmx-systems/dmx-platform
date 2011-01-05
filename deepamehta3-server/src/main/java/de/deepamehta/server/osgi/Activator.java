package de.deepamehta.server.osgi;

import de.deepamehta.core.service.CoreService;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class Activator implements BundleActivator {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String CORE_SERVICE_URI = "/core";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String bundleName;

    private ServiceTracker deepamehtaServiceTracker;
    private CoreService dms;

    private ServiceTracker httpServiceTracker;
    private HttpService httpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** BundleActivator Implementation ***
    // **************************************



    @Override
    public void start(BundleContext context) {
        bundleName = (String) context.getBundle().getHeaders().get("Bundle-Name");
        logger.info("========== Starting bundle \"" + bundleName + "\" ==========");
        //
        deepamehtaServiceTracker = createDeepamehtaServiceTracker(context);
        deepamehtaServiceTracker.open();
        //
        httpServiceTracker = createHttpServiceTracker(context);
        httpServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        logger.info("========== Stopping bundle \"" + bundleName + "\" ==========");
        //
        httpServiceTracker.close();
        deepamehtaServiceTracker.close();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private ServiceTracker createDeepamehtaServiceTracker(BundleContext context) {
        return new ServiceTracker(context, CoreService.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                logger.info("Adding DeepaMehta core service to bundle \"" + bundleName + "\"");
                dms = (CoreService) super.addingService(serviceRef);
                return dms;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                if (service == dms) {
                    logger.info("Removing DeepaMehta core service from bundle \"" + bundleName + "\"");
                    dms = null;
                }
                super.removedService(ref, service);
            }
        };
    }

    private ServiceTracker createHttpServiceTracker(BundleContext context) {
        return new ServiceTracker(context, HttpService.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                logger.info("Adding HTTP service to bundle \"" + bundleName + "\"");
                httpService = (HttpService) super.addingService(serviceRef);
                registerRestResources();
                return httpService;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                if (service == httpService) {
                    logger.info("Removing HTTP service from bundle \"" + bundleName + "\"");
                    unregisterRestResources();
                    httpService = null;
                }
                super.removedService(ref, service);
            }
        };
    }

    // ---

    private void registerRestResources() {
        try {
            logger.info("Registering REST resources");
            //
            Dictionary initParams = new Hashtable();
            initParams.put("com.sun.jersey.config.property.packages", "de.deepamehta.server.resources");
            //
            httpService.registerServlet(CORE_SERVICE_URI, new ServletContainer(), initParams, null);
        } catch (Exception e) {
            throw new RuntimeException("REST resources can't be registered", e);
        }
    }

    private void unregisterRestResources() {
        logger.info("Unregistering REST resources");
        httpService.unregister(CORE_SERVICE_URI);
    }
}
