package de.deepamehta.core.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import de.deepamehta.core.impl.service.WebPublishingService;

import java.util.logging.Logger;



class HttpServiceTracker {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private HttpService httpService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    HttpServiceTracker(BundleContext context) {
        //
        ServiceTracker serviceTracker = new ServiceTracker(context, HttpService.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference serviceRef) {
                Object service = super.addingService(serviceRef);
                if (service instanceof HttpService) {
                    logger.info("Adding HTTP service to DeepaMehta 4 Core");
                    httpService = (HttpService) service;
                    new WebPublishingService(context, httpService);
                } else {

                }
                //
                return service;
            }

            @Override
            public void removedService(ServiceReference ref, Object service) {
                if (service == httpService) {
                    logger.info("Removing HTTP service from DeepaMehta 4 Core");
                    // ### TODO: unregister WebPublishingService
                    httpService = null;
                }
                super.removedService(ref, service);
            }
        };
        serviceTracker.open();
    }
}
