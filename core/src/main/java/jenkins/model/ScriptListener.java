package jenkins.model;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.User;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.util.Listeners;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A listener to track Groovy scripts.
 *
 * @see Jenkins#_doScript(StaplerRequest, org.kohsuke.stapler.StaplerResponse, javax.servlet.RequestDispatcher, hudson.remoting.VirtualChannel, hudson.security.ACL)
 * @see hudson.cli.GroovyCommand#run()
 * @see hudson.cli.GroovyshCommand#run()
 */
public interface ScriptListener extends ExtensionPoint {

    /**
     * Called when a (privileged) groovy script is executed.
     *
     * @see Jenkins#_doScript(StaplerRequest, org.kohsuke.stapler.StaplerResponse, javax.servlet.RequestDispatcher, hudson.remoting.VirtualChannel, hudson.security.ACL)
     * @param script The script to be executed.
     * @param origin Descriptive identifier of the origin where the script is executed (Controller, Agent ID, Run ID).
     * @param u If available, the user that executed the script. Can be null.
     */
    void onScript(String script, String origin, User u);

    /**
     * Fires the {@link #onScript(String, String, User)} event to track the usage of the script console.
     *
     * @see Jenkins#_doScript(StaplerRequest, org.kohsuke.stapler.StaplerResponse, javax.servlet.RequestDispatcher, hudson.remoting.VirtualChannel, hudson.security.ACL)
     * @param script The script to be executed.
     * @param origin Descriptive identifier of the origin where the script is executed (Controller, Agent ID, Run ID).
     * @param u If available, the user that executed the script.
     */
    static void fireScriptEvent(String script, String origin, User u) {
        Listeners.notify(ScriptListener.class, true, listener -> listener.onScript(script, origin, u));
    }

    @Extension
    @Restricted(NoExternalUse.class)
    class LoggingListener implements ScriptListener {
        public static final Logger LOGGER = Logger.getLogger(LoggingListener.class.getName());

        @Override
        public void onScript(String script, String origin, User u) {
            LOGGER.log(Level.FINE, () -> "Script: '" + script + "' from origin: '" + origin + "' by user: '" + u + "'");
        }
    }
}
