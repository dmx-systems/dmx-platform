package systems.dmx.core.impl;

import systems.dmx.core.service.Transactional;
import systems.dmx.core.storage.spi.DMXTransaction;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



class TransactionFactory implements ResourceFilterFactory {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AccessLayer al;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ------------------------------------------------------------------------------------------------- Class Variables

    private static final ThreadLocal<DMXTransaction> threadLocalTransaction = new ThreadLocal();

    // ---------------------------------------------------------------------------------------------------- Constructors

    TransactionFactory(AccessLayer al) {
        this.al = al;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public List<ResourceFilter> create(AbstractMethod method) {
        if (!method.isAnnotationPresent(Transactional.class)) {
            return null;
        }
        //
        logger.fine("### Adding transaction support to " + info(method));
        List<ResourceFilter> filters = new ArrayList();
        filters.add(new TransactionResourceFilter(method));
        return filters;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String info(AbstractMethod method) {
        Method m = method.getMethod();
        return m.getDeclaringClass().getName() + "#" + m.getName() + "()";
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    private class TransactionResourceFilter implements ResourceFilter {

        private AbstractMethod method;

        private TransactionResourceFilter(AbstractMethod method) {
            this.method = method;
        }

        @Override
        public ContainerRequestFilter getRequestFilter() {
            return new ContainerRequestFilter() {

                @Override
                public ContainerRequest filter(ContainerRequest request) {
                    logger.fine("### Begining transaction of " + info(method));
                    DMXTransaction tx = al.db.beginTx();
                    threadLocalTransaction.set(tx);
                    return request;
                }
            };
        }

        @Override
        public ContainerResponseFilter getResponseFilter() {
            return new ContainerResponseFilter() {

                @Override
                public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                    DMXTransaction tx = threadLocalTransaction.get();
                    boolean success = response.getMappedThrowable() == null;
                    if (success) {
                        logger.fine("### Comitting transaction of " + info(method));
                        tx.success();
                    } else {
                        logger.warning("### Rollback transaction of " + info(method));
                    }
                    tx.finish();
                    return response;
                }
            };
        }
    }
}
