/*
 * The MIT License
 *
 * Copyright (c) 2023, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.console;


import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.model.Run;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;

/**
 * Extension point that allows implementations to redirect build console links to a specified URL.
 * <p>In order to produce links to console URLs in Jelly templates, use {@link Functions#getConsoleUrl()}.
 * @see Functions#getConsoleUrl
 * @since TODO
 */
public interface ConsoleUrlProvider extends ExtensionPoint {
    @Restricted(NoExternalUse.class)
    Logger LOGGER = Logger.getLogger(ConsoleUrlProvider.class.getName());

    /**
     * Get a URL relative to the context path of Jenkins which should be used to link to the console for the specified build.
     * <p>Should only be used in the context of serving an HTTP request.
     * @param run the build
     * @return the URL for the console for the specified build, relative to the context of Jenkins, or {@code null}
     * if this implementation does not want to server a special console view for this build.
     */
    @CheckForNull String getConsoleUrl(Run<?, ?> run);

    /**
     * Get a URL relative to the web server root which should be used to link to the console for the specified build.
     * <p>Should only be used in the context of serving an HTTP request.
     * <p>Use {@link Functions#getConsoleUrl} to obtain this link in a Jelly template.
     * @param run the build
     * @return the URL for the console for the specified build, relative to the web server root
     */
    static @NonNull String getRedirectUrl(Run<?, ?> run) {
        String url = null;
        for (ConsoleUrlProvider provider : ExtensionList.lookup(ConsoleUrlProvider.class)) {
            try {
                url = provider.getConsoleUrl(run);
                if (url != null) {
                    break;
                }
            } catch (Exception e) { // Intentionally broad catch clause to guard against broken implementations.
                LOGGER.log(Level.WARNING, e, () -> "Error looking up console URL for " + run + " from " + provider.getClass());
            }
        }
        if (url == null) {
            url = run.getUrl() + "console";
        }
        // TODO:
        // * Fail if absolute URL?
        // * Fail if invalid URI (as above in getActionUrl?).
        if (url.startsWith("/")) {
            return Stapler.getCurrentRequest().getContextPath() + url;
        } else {
            return Stapler.getCurrentRequest().getContextPath() + '/' + url;
        }
    }
}
