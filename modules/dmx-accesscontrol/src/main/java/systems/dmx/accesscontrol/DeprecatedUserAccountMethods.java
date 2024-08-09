package systems.dmx.accesscontrol;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import systems.dmx.core.Topic;
import systems.dmx.core.service.accesscontrol.Credentials;


@Deprecated
public interface DeprecatedUserAccountMethods {

    Topic createUserAccount(Credentials cred);

    Topic _createUserAccount(Credentials cred) throws Exception;

    Topic createUsername(String userName);

    interface DeprecatedAccountManagementServiceCallable<T> {
        T call(DeprecatedUserAccountMethods deprecatedAccountManagementService) throws Exception;
    }

    static <V> V call(BundleContext bundleContext, DeprecatedAccountManagementServiceCallable<V> callable) {
        ServiceReference<?> sr = bundleContext.getServiceReference("systems.dmx.accountmanagement.AccountManagementService");
        try {
            DeprecatedUserAccountMethods service = (DeprecatedUserAccountMethods) bundleContext.getService(sr);
            return service != null ? callable.call(service) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bundleContext.ungetService(sr);
        }
    }

    class CallableException extends RuntimeException {
        final Exception causeFromCallable;
        CallableException(Exception cause) {
            super(cause);
            causeFromCallable = cause;
        }
    }

}
