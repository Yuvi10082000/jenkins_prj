/*
 * The MIT License
 *
 * Copyright (c) 2018, CloudBees, Inc.
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
package hudson.cli;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Represent the return value of a CLI command.
 *
 * Can be implemented by enum when you need to return custom code
 * @see StandardCLIReturnCode
 * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-32273">JENKINS-32273</a>
 * @since TODO
 */
public interface CLIReturnCode {
    /**
     * @return The desired exit code that respect the contract described in {@link CLIReturnCode}
     */
    int getCode();

    /**
     * @return A optional human-readable message explaining why this return code was sent.
     */
    @CheckForNull String getReason(@Nonnull Locale locale);
}
