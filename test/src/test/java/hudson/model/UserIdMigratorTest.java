/*
 * The MIT License
 *
 * Copyright (c) 2018 CloudBees, Inc.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.reactor.ReactorException;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class UserIdMigratorTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void migrateSimpleUser() throws InterruptedException, ReactorException, IOException {
        String userId = "fred";
        User fred = User.getById(userId, false);
        assertThat(fred.getFullName(), is("Fred Smith"));
    }

    @Test
    @LocalData
    public void migrateMultipleUsers() throws InterruptedException, ReactorException, IOException {
        assertThat(User.getAll().size(), is(3));
        User fred = User.getById("fred", false);
        assertThat(fred.getFullName(), is("Fred Smith"));
        User legacyUser = User.getById("foo/bar", false);
        assertThat(legacyUser.getFullName(), is("Foo Bar"));
        User oldLegacyUser = User.getById("zzz\u1000", false);
        assertThat(oldLegacyUser.getFullName(), is("Old Legacy"));
    }

}
