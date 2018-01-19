package jenkins;

import hudson.FilePath.FileCallable;
import jenkins.security.Roles;
import org.jenkinsci.remoting.RoleChecker;

/**
 * {@link FileCallable}s that are meant to be only used on the master.
 *
 * @since 1.587 / 1.580.1
 * @param <T> the return type; note that this must either be defined in your plugin or included in the stock JEP-200 whitelist
 */
public abstract class MasterToSlaveFileCallable<T> implements FileCallable<T> {
    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, Roles.SLAVE);
    }
    private static final long serialVersionUID = 1L;
}
