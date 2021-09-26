/*
 * The MIT License
 *
 * Copyright 2015 CloudBees, Inc.
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

package jenkins.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.LogRotator;
import java.io.StringReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class BuildDiscarderPropertyTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue("JENKINS-31518")
    @LocalData
    @Test
    public void buildDiscarderField() throws Exception {
        FreeStyleProject p = r.jenkins.getItemByFullName("p", FreeStyleProject.class);
        verifyBuildDiscarder(p);
        r.configRoundtrip(p);
        verifyBuildDiscarder(p);
        String xml = p.getConfigFile().asString();
        assertFalse(xml, xml.contains("<logRotator class="));
        assertTrue(xml, xml.contains("<" + BuildDiscarderProperty.class.getName() + ">"));
    }

    private void verifyBuildDiscarder(FreeStyleProject p) {
        BuildDiscarder bd = p.getBuildDiscarder();
        assertNotNull(bd);
        LogRotator lr = (LogRotator) bd;
        assertEquals(7, lr.getDaysToKeep());
        assertEquals(10, lr.getNumToKeep());
        assertNotNull(p.getProperty(BuildDiscarderProperty.class));
        assertEquals(1, p.getProperties().size());
    }

    @Issue("JENKINS-16979")
    @LocalData
    @Test
    public void logRotatorField() throws Exception {
        AbstractProject<?,?> p = r.jenkins.getItemByFullName("foo", AbstractProject.class);
        verifyLogRotatorSanity(p);

        // now persist in the new format
        p.save();
        String xml = p.getConfigFile().asString();

        // make sure this new format roundtrips by itself
        p.setBuildDiscarder(null);
        p.updateByXml((Source) new StreamSource(new StringReader(xml)));
        verifyLogRotatorSanity(p);

        // another sanity check
        assertTrue(xml, xml.contains("<logRotator class=\"" + LogRotator.class.getName() + "\">"));
    }

    private static void verifyLogRotatorSanity(AbstractProject<?,?> p) {
        LogRotator d = (LogRotator) p.getBuildDiscarder();
        assertEquals(4, d.getDaysToKeep());
        assertEquals(3, d.getNumToKeep());
        assertEquals(2, d.getArtifactDaysToKeep());
        assertEquals(1, d.getArtifactNumToKeep());
    }
}
