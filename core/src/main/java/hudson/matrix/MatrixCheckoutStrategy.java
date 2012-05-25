package hudson.matrix;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.scm.SCM;
import hudson.tasks.BuildWrapper;

import java.io.File;
import java.io.IOException;

/**
 * Controls the check out behavior in the matrix project.
 * 
 * <p>
 * This extension point can be used to control the check out behaviour in matrix projects. The intended use cases
 * include situations like:
 * 
 * <ul>
 *     <li>Check out will only happen once in {@link MatrixBuild}, and its state will be then sent
 *         to {@link MatrixRun}s by other means such as rsync.
 *     <li>{@link MatrixBuild} does no check out of its own, and check out is only done on {@link MatrixRun}s
 * </ul>
 * 
 * <h2>Hook Semantics</h2>
 * There are currently two hooks defined on this class:
 * 
 * <h3>pre checkout</h3>
 * <p>
 * The default implementation calls into {@link BuildWrapper#preCheckout(AbstractBuild, Launcher, BuildListener)} calls.
 * You can override this method to do something before/after this, but you must still call into the {@code super.preCheckout}
 * so that matrix projects can satisfy the contract with {@link BuildWrapper}s.
 *
 * <h3>checkout</h3>
 * <p>
 * The default implementation uses {@link AbstractProject#checkout(AbstractBuild, Launcher, BuildListener, File)} to
 * let {@link SCM} do check out, but your {@link MatrixCheckoutStrategy} impls can substitute this call with other
 * operations that substitutes this semantics.
 * 
 * <h2>State and concurrency</h2>
 * <p>
 * An instance of this object gets created for a project for which this strategy is configured, so
 * the subtype needs to avoid using instance variables to refer to build-specific state (such as {@link BuildListener}s.)
 * Similarly, methods can be invoked concurrently. The code executes on the master, even if builds are running remotely.
 */
public abstract class MatrixCheckoutStrategy extends
    AbstractDescribableImpl<MatrixCheckoutStrategy> implements ExtensionPoint {

    /*
        Default behavior is defined in AbstractBuild.AbstractRunner, which is the common
        implementation for not just matrix projects but all sorts of other project types.
     */

    /**
     * Performs the pre checkout step.
     * 
     * This method is called by the {@link Executor} that's carrying out the build.
     * 
     * @param build
     *      Build being in progress. Never null.
     * @param launcher
     *      Allows you to launch process on the node where the build is actually running. Never null.
     * @param listener
     *      Allows you to write to console output and report errors. Never null.
     */
    public void preCheckout(MatrixRun build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        build.runner.defaultPreCheckout();
    }

    /**
     * Performs the checkout step.
     * 
     * See {@link #preCheckout(MatrixRun, Launcher, BuildListener)} for the semantics of the parameters.
     */
    public void checkout(MatrixRun build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        build.runner.defaultCheckout();
    }

    /**
     * Performs the pre-checkout step.
     *
     * See {@link #preCheckout(MatrixRun, Launcher, BuildListener)} for the semantics of the parameters.
     */
    public void preCheckout(MatrixBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        build.runner.defaultPreCheckout();
    }

    /**
     * Performs the checkout step.
     *
     * See {@link #preCheckout(MatrixRun, Launcher, BuildListener)} for the semantics of the parameters.
     */
    public void checkout(MatrixBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        build.runner.defaultCheckout();
    }

    @Override
    public MatrixCheckoutStrategyDescriptor getDescriptor() {
        return (MatrixCheckoutStrategyDescriptor)super.getDescriptor();
    }

}
