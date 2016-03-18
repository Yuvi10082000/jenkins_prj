package jenkins.install;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.BulkChange;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import hudson.security.csrf.DefaultCrumbIssuer;
import hudson.util.HttpResponses;
import hudson.util.PluginServletFilter;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;

/**
 * A Jenkins instance used during first-run to provide a limited set of services while
 * initial installation is in progress
 * 
 * @since 2.0
 */
public class SetupWizard {
    /**
     * The security token parameter name
     */
    public static String initialSetupAdminUserName = "admin";

    private final Logger LOGGER = Logger.getLogger(SetupWizard.class.getName());

    public SetupWizard(Jenkins j) throws IOException {
        // Create an admin user by default with a 
        // difficult password
        if(j.getSecurityRealm() == null || j.getSecurityRealm() == SecurityRealm.NO_AUTHENTICATION) { // this seems very fragile
            BulkChange bc = new BulkChange(j);
            
            HudsonPrivateSecurityRealm securityRealm = new HudsonPrivateSecurityRealm(false, false, null);
            j.setSecurityRealm(securityRealm);
            String randomUUID = UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ENGLISH);
            
            // create an admin user
            securityRealm.createAccount(SetupWizard.initialSetupAdminUserName, randomUUID);
            
            // JENKINS-33599 - write to a file in the jenkins home directory
            FileUtils.write(getInitialAdminPasswordFile(), randomUUID);
            
            // Lock Jenkins down:
            FullControlOnceLoggedInAuthorizationStrategy authStrategy = new FullControlOnceLoggedInAuthorizationStrategy();
            authStrategy.setAllowAnonymousRead(false);
            j.setAuthorizationStrategy(authStrategy);

            // Shut down all the ports we can by default:
            j.setSlaveAgentPort(-1); // -1 to disable
            
            // require a crumb issuer
            j.setCrumbIssuer(new DefaultCrumbIssuer(false));
            
            // set master -> slave security:
            j.getInjector().getInstance(AdminWhitelistRule.class)
                .setMasterKillSwitch(false);
            
            try{
                j.save(); // !!
            } finally {
                bc.commit();
            }
        }
        
        String setupKey = FileUtils.readFileToString(getInitialAdminPasswordFile());
        
        LOGGER.info("\n\n*************************************************************\n"
                + "*************************************************************\n"
                + "*************************************************************\n"
                + "\n"
                + "Jenkins initial setup is required. An admin user has been created and"
                + "a password generated. \n"
                + "Please use the following password to proceed to installation: \n"
                + "\n"
                + "" + setupKey + "\n"
                + "\n"
                + "This may also be found at: " + getInitialAdminPasswordFile().getCanonicalPath() + "\n"
                + "\n"
                + "*************************************************************\n"
                + "*************************************************************\n"
                + "*************************************************************\n");
        
        try {
            PluginServletFilter.addFilter(FORCE_SETUP_WIZARD_FILTER);
        } catch (ServletException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Gets the file used to store the initial admin password
     */
    public File getInitialAdminPasswordFile() {
        return new File(Jenkins.getInstance().root, "initialAdminPassword");
    }

    /**
     * Remove the setupWizard filter, ensure all updates are written to disk, etc
     */
    public HttpResponse doCompleteInstall(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Jenkins j = Jenkins.getInstance();
        j.setInstallState(InstallState.INITIAL_SETUP_COMPLETED);
        InstallUtil.saveLastExecVersion();
        PluginServletFilter.removeFilter(FORCE_SETUP_WIZARD_FILTER);
        // Also, clean up the setup wizard if it's completed
        j.setSetupWizard(null);
        return HttpResponses.okJSON();
    }
    
    /**
     * This filter will validate that the security token is provided
     */
    private final Filter FORCE_SETUP_WIZARD_FILTER = new Filter() {
        @Override
        public void init(FilterConfig cfg) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            // As an extra measure of security, the install wizard generates a security token, and
            // requires the user to enter it before proceeding through the installation. Once set
            // we'll set a cookie so the subsequent operations succeed
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest)request;
                //if (!Pattern.compile(".*[.](css|ttf|gif|woff|eot|png|js)").matcher(req.getRequestURI()).matches()) {
                    // Allow js & css requests through
                if((req.getContextPath() + "/").equals(req.getRequestURI())) {
                    chain.doFilter(new HttpServletRequestWrapper(req) {
                        public String getRequestURI() {
                            return getContextPath() + "/setupWizard/";
                        }
                    }, response);
                    return;
                }
                // fall through to handling the request normally
            }
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    };
}
