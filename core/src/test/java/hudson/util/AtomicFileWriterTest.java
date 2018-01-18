package hudson.util;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import static org.hamcrest.core.StringContains.*;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

public class AtomicFileWriterTest {
    private static final String PREVIOUS = "previous value \n blah";
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    File af;
    AtomicFileWriter afw;
    String expectedContent = "hello world";

    @Before
    public void setUp() throws IOException {
        af = tmp.newFile();
        FileUtils.writeStringToFile(af, PREVIOUS);
        afw = new AtomicFileWriter(af.toPath(), Charset.defaultCharset());
    }

    @Test
    public void symlinkToDirectory() throws Exception {
        final File folder = tmp.newFolder();
        final File containingSymlink = tmp.newFolder();
        final Path zeSymlink = Files.createSymbolicLink(Paths.get(containingSymlink.getAbsolutePath(), "ze_symlink"),
                                                         folder.toPath());


        final Path childFileInSymlinkToDir = Paths.get(zeSymlink.toString(), "childFileInSymlinkToDir");

        new AtomicFileWriter(childFileInSymlinkToDir, Charset.forName("UTF-8"));
    }

    @Test
    public void createFile() throws Exception {
        // Verify the file we created exists
        assertTrue(Files.exists(afw.getTemporaryPath()));
    }

    @Test
    public void writeToAtomicFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());
        afw.write(expectedContent);
        afw.write(' ');

        // When
        afw.flush();

        // Then
        assertEquals("File writer did not properly flush to temporary file",
                expectedContent.length()*2+1, Files.size(afw.getTemporaryPath()));
    }

    @Test
    public void commitToFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());
        afw.write(new char[]{'h', 'e', 'y'}, 0, 3);

        // When
        afw.commit();

        // Then
        assertEquals(expectedContent.length()+3, Files.size(af.toPath()));
        assertEquals(expectedContent+"hey", FileUtils.readFileToString(af));
    }

    @Test
    public void abortDeletesTmpFile() throws Exception {
        // Given
        afw.write(expectedContent, 0, expectedContent.length());

        // When
        afw.abort();

        // Then
        assertTrue(Files.notExists(afw.getTemporaryPath()));
        assertEquals(PREVIOUS, FileUtils.readFileToString(af));
    }

    @Test
    public void indexOutOfBoundsLeavesOriginalUntouched() throws Exception {
        // Given
        try {
            afw.write(expectedContent, 0, expectedContent.length() + 10);
            fail("exception expected");
        } catch (IndexOutOfBoundsException e) {
        }

        assertEquals(PREVIOUS, FileUtils.readFileToString(af));
    }
    @Test
    public void badPath() throws Exception {
        final File newFile = tmp.newFile();
        File parentExistsAndIsAFile = new File(newFile, "badChild");

        assertTrue(newFile.exists());
        assertFalse(parentExistsAndIsAFile.exists());

        try {
            new AtomicFileWriter(parentExistsAndIsAFile.toPath(), Charset.forName("UTF-8"));
            fail("Expected a failure");
        } catch (IOException e) {
            assertThat(e.getMessage(),
                       containsString("exists and is neither a directory nor a symlink to a directory"));
        }
    }

    @Issue("JENKINS-48407")
    @Test
    public void checkPermissions() throws IOException, InterruptedException {

        final File newFile = tmp.newFile();

        // Check Posix calls are supported (to avoid running this test on Windows for instance)
        boolean posixSupported = true;
        try {
            Files.getPosixFilePermissions(newFile.toPath());
        } catch (UnsupportedOperationException e) {
            posixSupported = false;
        }
        assumeThat(posixSupported, is(true));

        // given
        final Set<PosixFilePermission> givenPermissions = EnumSet.of(OWNER_READ,
                                                                     OWNER_WRITE,
                                                                     GROUP_READ,
                                                                     GROUP_WRITE,
                                                                     OTHERS_READ
        );

        final Set<PosixFilePermission> notGivenPermissions = EnumSet.of(OWNER_EXECUTE,
                                                                        GROUP_EXECUTE,
                                                                        OTHERS_WRITE,
                                                                        OTHERS_EXECUTE);

        Files.setPosixFilePermissions(newFile.toPath(), givenPermissions);
        Path filePath = newFile.toPath();

        // when
        AtomicFileWriter w = new AtomicFileWriter(filePath, StandardCharsets.UTF_8);
        w.write("whatever");
        w.commit();

        // then
        assertFalse(w.getTemporaryPath().toFile().exists());
        assertTrue(filePath.toFile().exists());

        final Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(filePath);

        for (PosixFilePermission perm : givenPermissions) {
            assertTrue("missing: " + perm, posixFilePermissions.contains(perm));
        }

        for (PosixFilePermission perm : notGivenPermissions) {
            assertFalse("should not be allowed: " + perm, posixFilePermissions.contains(perm));
        }
    }
}
