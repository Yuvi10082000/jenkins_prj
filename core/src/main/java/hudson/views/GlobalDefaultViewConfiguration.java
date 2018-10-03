/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
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
package hudson.views;

import hudson.Extension;
import hudson.model.View;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Adds the default view configuration to the system config page.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension(ordinal=300) @Symbol("defaultView")
public class GlobalDefaultViewConfiguration extends GlobalConfiguration {

    public String getPrimaryViewName() {
        return Jenkins.get().getPrimaryView().getViewName();
    }

    @DataBoundSetter
    public void setPrimaryViewName(String viewName) throws FormException {
        final Jenkins j = Jenkins.get();
        if (viewName != null &&j.getView(viewName) == null) {
            throw new FormException(Messages.GlobalDefaultViewConfiguration_ViewDoesNotExist(viewName), "primaryView");
        }
        j.setPrimaryView(viewName);
    }
}


