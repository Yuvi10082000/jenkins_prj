/*
 * The MIT License
 *
 * Copyright 2013 Jesse Glick.
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

import static org.junit.Assert.assertEquals;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.LogTaskListener;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

public class RunParameterDefinitionTest {

    private static final Logger LOGGER = Logger.getLogger(Run.class.getName());

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Bug(16462)
    @Test
    public void inFolders() throws Exception {
        MockFolder dir = j.createFolder("dir");
        MockFolder subdir = dir.createProject(MockFolder.class, "sub dir");
        FreeStyleProject p = subdir.createProject(FreeStyleProject.class, "some project");
        p.scheduleBuild2(0).get();
        FreeStyleBuild build2 = p.scheduleBuild2(0).get();
        p.scheduleBuild2(0).get();
        String id = build2.getExternalizableId();
        assertEquals("dir/sub dir/some project#2", id);
        assertEquals(build2, Run.fromExternalizableId(id));
        RunParameterDefinition def = new RunParameterDefinition("build", "dir/sub dir/some project", "my build", null);
        assertEquals("dir/sub dir/some project", def.getProjectName());
        assertEquals(p, def.getProject());
        EnvVars env = new EnvVars();
        def.getDefaultParameterValue().buildEnvVars(null, env);
        assertEquals(j.jenkins.getRootUrl() + "job/dir/job/sub%20dir/job/some%20project/3/", env.get("build"));
        RunParameterValue val = def.createValue(id);
        assertEquals(build2, val.getRun());
        assertEquals("dir/sub dir/some project", val.getJobName());
        assertEquals("2", val.getNumber());
        val.buildEnvVars(null, env);
        assertEquals(j.jenkins.getRootUrl() + "job/dir/job/sub%20dir/job/some%20project/2/", env.get("build"));
        assertEquals("dir/sub dir/some project", env.get("build.jobName"));
        assertEquals("dir/sub dir/some project", env.get("build_JOBNAME"));
        assertEquals("2", env.get("build.number"));
        assertEquals("2", env.get("build_NUMBER"));
    }

    @Test
    public void testNULLThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             null));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(project.getLastBuild().getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }

    
    @Test
    public void testALLThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "ALL"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(project.getLastBuild().getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }

    @Test
    public void testABORTEDThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "ABORTED"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(abortedBuild.getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }
    
    @Test
    public void testNOT_BUILTThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "NOT_BUILT"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(notBuiltBuild.getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }
    
    @Test
    public void testFAILUREThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "FAILURE"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(failedBuild.getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }
    
    @Test
    public void testUNSTABLEThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "UNSTABLE"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(unstableBuild.getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }
    
    @Test
    public void testSUCCESSThreshold() throws Exception {

        FreeStyleProject project = j.createFreeStyleProject("project");
        FreeStyleBuild successfulBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.UNSTABLE)));
        FreeStyleBuild unstableBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.FAILURE)));
        FreeStyleBuild failedBuild = project.scheduleBuild2(0).get();

        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.NOT_BUILT)));
        FreeStyleBuild notBuiltBuild = project.scheduleBuild2(0).get();
        
        project.getPublishersList().replaceBy(Collections.singleton(new ResultPublisher(Result.ABORTED)));
        FreeStyleBuild abortedBuild = project.scheduleBuild2(0).get();

        FreeStyleProject paramProject = j.createFreeStyleProject("paramProject");
        ParametersDefinitionProperty pdp = 
                new ParametersDefinitionProperty(new RunParameterDefinition("RUN", 
                                                                             project.getName(),
                                                                             "run description",
                                                                             "SUCCESS"));
        paramProject.addProperty(pdp);

        FreeStyleBuild build = paramProject.scheduleBuild2(0).get();
        assertEquals(Integer.toString(successfulBuild.getNumber()),
                     build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO)).get("RUN_NUMBER"));
    }
    
    
    static class ResultPublisher extends Publisher {

        private Result result = Result.FAILURE;

        public ResultPublisher(Result result) {
            this.result = result;
        }

        public @Override
        boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            build.setResult(result);
            return true;
        }

        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }

        public Descriptor<Publisher> getDescriptor() {
            return new Descriptor<Publisher>(ResultPublisher.class) {
                public String getDisplayName() {
                    return "ResultPublisher";
                }
            };
        }
    }
}
