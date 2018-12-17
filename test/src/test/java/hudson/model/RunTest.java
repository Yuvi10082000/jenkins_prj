/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jorg Heymans
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
package hudson.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.slaves.DumbSlave;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;

import hudson.FilePath;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import jenkins.model.ArtifactManager;
import jenkins.model.ArtifactManagerConfiguration;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.ArtifactManagerFactoryDescriptor;
import jenkins.model.Jenkins;
import jenkins.util.VirtualFile;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

public class RunTest  {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Issue("JENKINS-17935") @SuppressWarnings("deprecation")
    @Test public void getDynamicInvisibleTransientAction() throws Exception {
        TransientBuildActionFactory.all().add(0, new TransientBuildActionFactory() {
            @Override public Collection<? extends Action> createFor(Run target) {
                return Collections.singleton(new Action() {
                    @Override public String getDisplayName() {
                        return "Test";
                    }
                    @Override public String getIconFileName() {
                        return null;
                    }
                    @Override public String getUrlName() {
                        return null;
                    }
                });
            }
        });
        j.assertBuildStatusSuccess(j.createFreeStyleProject("stuff").scheduleBuild2(0));
        j.createWebClient().assertFails("job/stuff/1/nonexistent", HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test public void doNotOverrideCharacteristicBuildEnvVar() throws Exception {
        FreeStyleProject p = setupJobOnSlaveWithEnv("BUILD_NUMBER", "FROM_SLAVE");

        ContributingExtension.values("BUILD_NUMBER", "FROM_CONTRIBUTOR");
        p.getBuildWrappersList().add(new ContributingWrapper("BUILD_NUMBER", "FROM_WRAPPER"));
        p.getBuildersList().add(new ContributingBuilder("BUILD_NUMBER", "FROM_CONTRIBUTOR"));

        assertEnvVar(p, "BUILD_NUMBER", "1");
    }

    @Test public void doNotOverrideCharacteristicJobEnvVar() throws Exception {
        FreeStyleProject p = setupJobOnSlaveWithEnv("JOB_NAME", "FROM_SLAVE");

        ContributingExtension.values("JOB_NAME", "FROM_CONTRIBUTOR");
        p.getBuildWrappersList().add(new ContributingWrapper("JOB_NAME", "FROM_WRAPPER"));
        p.getBuildersList().add(new ContributingBuilder("JOB_NAME", "FROM_CONTRIBUTOR"));

        assertEnvVar(p, "JOB_NAME", "job_name");
    }

    @Test public void doNotOverrideBuildWrapperEnvVar() throws Exception {
        FreeStyleProject p = setupJobOnSlaveWithEnv("DISPLAY", "SLAVE_VAL");

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        assertEnvVar(p, "DISPLAY", "BUILD_VAL");
    }

    @Test public void doNotOverrideBuilderEnvVar() throws Exception {
        FreeStyleProject p = setupJobOnSlaveWithEnv("DISPLAY", "SLAVE_VAL");

        p.getBuildersList().add(new ContributingBuilder("DISPLAY", "BUILD_VAL"));

        assertEnvVar(p, "DISPLAY", "BUILD_VAL");
    }

    @Test public void doNotOverrideContributorEnvVar() throws Exception {
        FreeStyleProject p = setupJobOnSlaveWithEnv("DISPLAY", "SLAVE_VAL");

        ContributingExtension.values("DISPLAY", "BUILD_VAL");

        assertEnvVar(p, "DISPLAY", "BUILD_VAL");
    }

    private FreeStyleProject setupJobOnSlaveWithEnv(String name, String value) throws Exception {
        DumbSlave slave = slaveContributing(name, value);
        FreeStyleProject p = j.createFreeStyleProject("job_name");
        p.setAssignedNode(slave);
        return p;
    }

    @SuppressWarnings("deprecation")
    private void assertEnvVar(FreeStyleProject p, String key, String value) throws Exception {
        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);
        j.buildAndAssertSuccess(p);
        assertThat(capture.getEnvVars(), hasEntry(key, value));
    }

    private DumbSlave slaveContributing(String key, String value) throws Exception {
        return j.createOnlineSlave(null, new EnvVars(key, value));
    }

    private static final class ContributingWrapper extends BuildWrapper {
        private final String value;
        private final String key;

        private ContributingWrapper(String key, String value) {
            this.value = value;
            this.key = key;
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.put(key, value);
                }
            };
        }

        @Extension
        public static class Descriptor extends hudson.model.Descriptor<BuildWrapper> {
            @Override
            public String getDisplayName() {
                return null;
            }
        }
    }

    private static final class ContributingBuilder extends Builder {
        private final String value;
        private final String key;

        private ContributingBuilder(String key, String value) {
            this.value = value;
            this.key = key;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            build.addAction(new EnvironmentContributingAction() {

                public String getUrlName() {
                    return null;
                }

                public String getIconFileName() {
                    return null;
                }

                public String getDisplayName() {
                    return null;
                }

                public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
                    env.put(key, value);
                }
            });
            return true;
        }

        @Extension
        public static class Descriptor extends hudson.model.Descriptor<Builder> {
            @Override
            public String getDisplayName() {
                return null;
            }
        }
    }

    @TestExtension
    public static final class ContributingExtension extends EnvironmentContributor {
        private String value;
        private String key;

        private static void values(String k, String v) {
            ContributingExtension inst = ExtensionList.lookup(ContributingExtension.class).get(0);
            inst.value = v;
            inst.key = k;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void buildEnvironmentFor(
                Run r, EnvVars envs, TaskListener listener
        ) throws IOException, InterruptedException {
            if (key != null) {
                envs.put(key, value);
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void buildEnvironmentFor(
                Job j, EnvVars envs, TaskListener listener
        ) throws IOException, InterruptedException {
            if (key != null) {
                envs.put(key, value);
            }
        }
    }

    @Issue("JENKINS-40281")
    @Test public void getBadgeActions() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        assertEquals(0, b.getBadgeActions().size());
        assertTrue(b.canToggleLogKeep());
        b.keepLog();
        List<BuildBadgeAction> badgeActions = b.getBadgeActions();
        assertEquals(1, badgeActions.size());
        assertEquals(Run.KeepLogBuildBadge.class, badgeActions.get(0).getClass());
    }

    @Issue("JENKINS-51819")
    @Test public void deleteArtifactsCustom() throws Exception {
        ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(new Mgr.Factory());
        FreeStyleProject p = j.createFreeStyleProject();
        j.jenkins.getWorkspaceFor(p).child("f").write("", null);
        p.getPublishersList().add(new ArtifactArchiver("f"));
        FreeStyleBuild b = j.buildAndAssertSuccess(p);
        b.delete();
        assertTrue(Mgr.deleted.get());
    }
    private static final class Mgr extends ArtifactManager {
        static final AtomicBoolean deleted = new AtomicBoolean();
        @Override public boolean delete() throws IOException, InterruptedException {
            return !deleted.getAndSet(true);
        }
        @Override public void onLoad(Run<?, ?> build) {}
        @Override public void archive(FilePath workspace, Launcher launcher, BuildListener listener, Map<String, String> artifacts) throws IOException, InterruptedException {}
        @Override public VirtualFile root() {
            return VirtualFile.forFile(Jenkins.get().getRootDir()); // irrelevant
        }
        public static final class Factory extends ArtifactManagerFactory {
            @DataBoundConstructor public Factory() {}
            @Override public ArtifactManager managerFor(Run<?, ?> build) {
                return new Mgr();
            }
            @TestExtension("deleteArtifactsCustom") public static final class DescriptorImpl extends ArtifactManagerFactoryDescriptor {}
        }
    }
}
