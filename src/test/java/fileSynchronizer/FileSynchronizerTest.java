package fileSynchronizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class FileSynchronizerTest {

    private final Path testingParentDirectory = Path.of(System.getProperty("user.home")).resolve("Desktop").resolve("filesync_test");
    private final Path testingLocalDirectory = testingParentDirectory.resolve("local");
    private final Path testingRemoteDirectory = testingParentDirectory.resolve("remote");

    private InputStream userInput = new ByteArrayInputStream(new byte[]{});

    void createFiles(Path parentDir, Path...relativePaths) {
        for (Path relative : relativePaths) {
            createFile(parentDir.resolve(relative));
        }
    }

    void createFile(Path absolutePath) {
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.createFile(absolutePath);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void createDirectories(Path parentDir, Path...relativePaths) {
        for (Path relative : relativePaths) {
            createDirectories(parentDir.resolve(relative));
        }
    }

    void createDirectories(Path absolutePath) {
        try {
            Files.createDirectories(absolutePath);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void deleteFiles(Path parentDir, Path...relativePaths) {
        for (Path relative : relativePaths) {
            deleteFile(parentDir.resolve(relative));
        }
    }

    void deleteFile(Path absolutePath) {
        try {
            Files.delete(absolutePath);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void appendLineToFile(Path filepath, String textToAppend) {
        try {
            Files.write(filepath, (textToAppend + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void passUserInput(String ... inputs) {
        StringBuilder allInputs = new StringBuilder();
        for (String token : inputs) {
            allInputs.append(token).append(System.lineSeparator());
        }

        userInput = new ByteArrayInputStream(allInputs.toString().getBytes());
    }

    String getFileContents(Path filepath) {
        StringBuilder allLines = new StringBuilder();

        List<String> lines = getFileLines(filepath);
        for (int i = 0; i < lines.size(); i++) {
            allLines.append(lines.get(i));
            if (i < lines.size() - 1) allLines.append(System.lineSeparator());
        }

        return allLines.toString();
    }

    List<String> getFileLines(Path filepath) {
        List<String> lines = new ArrayList<>();

        try {
            lines = Files.readAllLines(filepath);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }

        return lines;
    }

    boolean allFilesExist(Path parentDir, Path...paths) {
        for (Path p : paths) {
            if (!parentDir.resolve(p).toFile().exists()) return false;
        }
        return true;
    }

    FileSynchronizer testingFileSynchronizer(boolean isFirstSync, boolean performFullSync, boolean deleteTrash, String...conflictResolutions) {
        String[] inputs = new String[conflictResolutions.length + ((isFirstSync) ? 2 : 1)];

        // Load input stream with user responses
        if (isFirstSync) inputs[0] = (performFullSync) ? "1" : "2";
        inputs[inputs.length - 1] = (deleteTrash) ? "y" : "n";

        for (int i = 0; i < conflictResolutions.length; i++) {
            inputs[i + ((isFirstSync) ? 1 : 0)] = conflictResolutions[i];
        }

        passUserInput(inputs);
        return new FileSynchronizer(testingLocalDirectory.toString(), testingRemoteDirectory.toString(), "local", "remote", userInput, true);
    }

    @BeforeEach
    void setUpTestDirectories() {
        try {
            Files.createDirectory(testingParentDirectory);
            Files.createDirectory(testingLocalDirectory);
            Files.createDirectory(testingRemoteDirectory);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    @AfterEach
    void tearDownTestDirectories() {
        try {
            Files.walkFileTree(testingParentDirectory, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    System.err.println("TEST ERROR: Delete failed: '" + path + "'");
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }

    @Test
    void noUserFilesInEitherDirectory() {
        FileSynchronizer synchronizer = testingFileSynchronizer(true, true, true);
        synchronizer.synchronizeFileTrees();
        delay(10);

        File localDir = testingLocalDirectory.toFile();
        String[] localDirFilenames = localDir.list();
        boolean localHasUserFiles = false;
        for (String filename : localDirFilenames) {
            if (!filename.equals(".sync_trash") && !filename.equals(".sync_log") && !filename.equals(".sync_exclude")) {
                localHasUserFiles = true;
                break;
            }
        }

        File remoteDir = testingRemoteDirectory.toFile();
        String[] remoteDirFilenames = remoteDir.list();
        boolean remoteHasUserFiles = false;
        for (String filename : remoteDirFilenames) {
            if (!filename.equals(".sync_trash") && !filename.equals(".sync_log") && !filename.equals(".sync_exclude")) {
                remoteHasUserFiles = true;
                break;
            }
        }

        assertFalse(localHasUserFiles);
        assertFalse(remoteHasUserFiles);
    }

    @Test
    void newLocalFilesShouldBeCopiedToRemote() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("newLocalFile1.txt");
        Path newLocalFile2 = Path.of("newLocalFile2.txt");

        createFiles(testingLocalDirectory, newLocalFile1, newLocalFile2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void newRemoteFilesShouldBeCopiedToLocal() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("newRemoteFile1.txt");
        Path newLocalFile2 = Path.of("newRemoteFile2.txt");

        createFiles(testingRemoteDirectory, newLocalFile1, newLocalFile2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingLocalDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void newEmptyLocalDirectoriesShouldBeCopiedToRemote() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newLocalDir1 = Path.of("newLocalDir1");
        Path newLocalDir2 = Path.of("newLocalDir2");

        createDirectories(testingLocalDirectory, newLocalDir1, newLocalDir2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalDir1, newLocalDir2));
    }

    @Test
    void newEmptyRemoteDirectoriesShouldBeCopiedToRemote() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newRemoteDir1 = Path.of("newRemoteDir1");
        Path newRemoteDir2 = Path.of("newRemoteDir2");

        createDirectories(testingRemoteDirectory, newRemoteDir1, newRemoteDir2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingLocalDirectory, newRemoteDir1, newRemoteDir2));
    }

    @Test
    void newNonEmptyLocalDirectoryShouldBeCopiedToRemote() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newLocalDir1 = Path.of("newLocalDir1.txt");
        Path newLocalFile1 = newLocalDir1.resolve("newLocalFile1.txt");

        createDirectories(testingLocalDirectory, newLocalDir1);
        createFiles(testingLocalDirectory, newLocalFile1);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalDir1, newLocalFile1));
    }

    @Test
    void newNonEmptyRemoteDirectoryShouldBeCopiedToRemote() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newRemoteDir1 = Path.of("newRemoteDir1.txt");
        Path newRemoteFile1 = newRemoteDir1.resolve("newRemoteFile1.txt");

        createDirectories(testingRemoteDirectory, newRemoteDir1);
        createFiles(testingRemoteDirectory, newRemoteFile1);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingLocalDirectory, newRemoteDir1, newRemoteFile1));
    }

    @Test
    void modifiedLocalFilesShouldBeCopiedToRemote() {
        Path localFile1 = Path.of("modifiedLocalFile");
        Path localFile2 = Path.of("modifiedLocalFile2");
        createFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        appendLineToFile(testingLocalDirectory.resolve(localFile1), "AppendTest");
        appendLineToFile(testingLocalDirectory.resolve(localFile2), "AppendTest2");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertEquals("AppendTest", getFileContents(testingRemoteDirectory.resolve(localFile1)));
        assertEquals("AppendTest2", getFileContents(testingRemoteDirectory.resolve(localFile2)));
    }

    @Test
    void modifiedRemoteFilesShouldBeCopiedToLocal() {
        Path remoteFile1 = Path.of("modifiedRemoteFile");
        Path remoteFile2 = Path.of("modifiedRemoteFile2");
        createFiles(testingLocalDirectory, remoteFile1, remoteFile2);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        appendLineToFile(testingLocalDirectory.resolve(remoteFile1), "AppendTest");
        appendLineToFile(testingLocalDirectory.resolve(remoteFile2), "AppendTest2");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertEquals("AppendTest", getFileContents(testingRemoteDirectory.resolve(remoteFile1)));
        assertEquals("AppendTest2", getFileContents(testingRemoteDirectory.resolve(remoteFile2)));
    }

    @Test
    void fileWithSpaceInNameShouldSyncSuccessfully() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path newLocalFile1 = Path.of("new Local File1.txt");
        Path newLocalFile2 = Path.of("new Local File2.txt");

        createFiles(testingLocalDirectory, newLocalFile1, newLocalFile2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalFile1, newLocalFile2));
    }

    @Test
    void syncConflictShouldCopyLocalFileOverRemote() {
        Path conflict = Path.of("conflictFile");
        createFiles(testingLocalDirectory, conflict);
        createFiles(testingRemoteDirectory, conflict);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true, "1");
        firstSync.synchronizeFileTrees();
        delay(10);

        appendLineToFile(testingLocalDirectory.resolve(conflict), "Local");
        appendLineToFile(testingRemoteDirectory.resolve(conflict), "Remote");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true, "1");
        secondSync.synchronizeFileTrees();

        assertEquals("Local", getFileContents(testingLocalDirectory.resolve(conflict)));
        assertEquals("Local", getFileContents(testingRemoteDirectory.resolve(conflict)));
    }

    @Test
    void syncConflictShouldCopyRemoteFileOverLocal() {
        Path conflict = Path.of("conflictFile");
        createFiles(testingLocalDirectory, conflict);
        createFiles(testingRemoteDirectory, conflict);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true, "2");
        firstSync.synchronizeFileTrees();
        delay(10);

        appendLineToFile(testingLocalDirectory.resolve(conflict), "Local");
        appendLineToFile(testingRemoteDirectory.resolve(conflict), "Remote");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true, "2");
        secondSync.synchronizeFileTrees();

        assertEquals("Remote", getFileContents(testingLocalDirectory.resolve(conflict)));
        assertEquals("Remote", getFileContents(testingRemoteDirectory.resolve(conflict)));
    }

    @Test
    void deletedLocalFileShouldBeDeletedFromRemote() {
        Path localFile1 = Path.of("localFile1.txt");
        Path localFile2 = Path.of("localFile2.txt");

        createFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        deleteFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertFalse(allFilesExist(testingRemoteDirectory, localFile1));
        assertFalse(allFilesExist(testingRemoteDirectory, localFile2));
    }

    @Test
    void deletedRemoteFileShouldBeDeletedFromLocal() {
        Path remoteFile1 = Path.of("remoteFile1.txt");
        Path remoteFile2 = Path.of("remoteFile2.txt");

        createFiles(testingRemoteDirectory, remoteFile1, remoteFile2);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        deleteFiles(testingRemoteDirectory, remoteFile1, remoteFile2);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertFalse(allFilesExist(testingLocalDirectory, remoteFile1));
        assertFalse(allFilesExist(testingLocalDirectory, remoteFile2));
    }

    @Test
    void deletedFilesShouldAppearInTrash() {
        Path newLocalDir1 = Path.of("newLocalDir1");
        Path newLocalFile1 = newLocalDir1.resolve("newLocalFile1.txt");

        createDirectories(testingLocalDirectory, newLocalDir1);
        createFiles(testingLocalDirectory, newLocalFile1);

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        deleteFiles(testingLocalDirectory, newLocalFile1, newLocalDir1);

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, false);
        secondSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory.resolve(".sync_trash"), newLocalDir1, newLocalFile1));
    }

    @Test
    void filesInExclusionListShouldBeIgnored() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path excludedFile = Path.of("excluded");
        createFiles(testingLocalDirectory, excludedFile);
        appendLineToFile(testingLocalDirectory.resolve(".sync_exclude"), "excluded");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertFalse(allFilesExist(testingRemoteDirectory, excludedFile));
    }

    @Test
    void directoriesInExclusionListShouldBeIgnored() {
        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();
        delay(10);

        Path excludedDir = Path.of("excluded");
        Path excludedFile = excludedDir.resolve("excludedFile1");
        createDirectories(testingLocalDirectory, excludedDir);
        createFiles(testingLocalDirectory, excludedFile);
        appendLineToFile(testingLocalDirectory.resolve(".sync_exclude"), "excluded");

        FileSynchronizer secondSync = testingFileSynchronizer(false, true, true);
        secondSync.synchronizeFileTrees();

        assertFalse(allFilesExist(testingRemoteDirectory, excludedDir));
        assertFalse(allFilesExist(testingRemoteDirectory, excludedFile));
    }

    @Test
    void syncingWithTwoDifferentRemotesShouldStaySeparate() {
        Path localFile1 = Path.of("localFile1");
        Path localDir1 = Path.of("localDir1");
        Path localFile2 = localDir1.resolve("localFile2");

        createDirectories(testingLocalDirectory, localDir1);
        createFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer localToRemote = testingFileSynchronizer(true, true, true);
        localToRemote.synchronizeFileTrees();
        delay(10);

        Path backupDir = Path.of("backup");
        createDirectories(testingParentDirectory, backupDir);

        passUserInput("1");
        FileSynchronizer remoteToBackup = new FileSynchronizer(testingRemoteDirectory.toString(), testingParentDirectory.resolve(backupDir).toString(), "remote", "backup", userInput, true);
        remoteToBackup.synchronizeFileTrees();

        assertTrue(allFilesExist(testingParentDirectory.resolve(backupDir), localFile1, localFile2, localDir1));
    }

    @Test
    void syncLogShouldHaveOneHostAndSyncTimePerLine() {
        Path localFile1 = Path.of("localFile1");
        Path localDir1 = Path.of("localDir1");
        Path localFile2 = localDir1.resolve("localFile2");

        createDirectories(testingLocalDirectory, localDir1);
        createFiles(testingLocalDirectory, localFile1, localFile2);

        FileSynchronizer localToRemote = testingFileSynchronizer(true, true, true);
        localToRemote.synchronizeFileTrees();
        delay(10);

        Path backupDir = Path.of("backup");
        createDirectories(testingParentDirectory, backupDir);

        passUserInput("1");
        FileSynchronizer remoteToBackup = new FileSynchronizer(testingRemoteDirectory.toString(), testingParentDirectory.resolve(backupDir).toString(), "remote", "backup", userInput, true);
        remoteToBackup.synchronizeFileTrees();

        boolean oneHostAndTimePerLine = true;

        for (String line : getFileContents(testingParentDirectory.resolve(backupDir).resolve(".sync_log")).split(System.lineSeparator())) {
            oneHostAndTimePerLine &= (line.split(",").length == 2);
        }

        assertTrue(oneHostAndTimePerLine);
    }

    @Test
    void syncExcludeShouldBeSharedBetweenHosts() {
        Path syncExclude = Path.of(".sync_exclude");
        createFiles(testingLocalDirectory, syncExclude);
        createFiles(testingRemoteDirectory, syncExclude);

        appendLineToFile(testingLocalDirectory.resolve(syncExclude), "localFile1");
        appendLineToFile(testingLocalDirectory.resolve(syncExclude), "localFile2");

        appendLineToFile(testingRemoteDirectory.resolve(syncExclude), "remoteFile1");
        appendLineToFile(testingRemoteDirectory.resolve(syncExclude), "remoteFile2");

        FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();

        assertTrue(getFileLines(testingLocalDirectory.resolve(syncExclude)).contains("localFile1"));
        assertTrue(getFileLines(testingLocalDirectory.resolve(syncExclude)).contains("localFile2"));
        assertTrue(getFileLines(testingLocalDirectory.resolve(syncExclude)).contains("remoteFile1"));
        assertTrue(getFileLines(testingLocalDirectory.resolve(syncExclude)).contains("remoteFile2"));

        assertTrue(getFileLines(testingRemoteDirectory.resolve(syncExclude)).contains("remoteFile2"));
        assertTrue(getFileLines(testingRemoteDirectory.resolve(syncExclude)).contains("remoteFile2"));
        assertTrue(getFileLines(testingRemoteDirectory.resolve(syncExclude)).contains("remoteFile2"));
        assertTrue(getFileLines(testingRemoteDirectory.resolve(syncExclude)).contains("remoteFile2"));
    }
    
    @Test
    void localNestedExcludedFilesShouldBeIgnoredDuringSync() {
    	Path newLocalDir = Path.of("newLocalDir");
    	Path newLocalFile = Path.of("newLocalFile");
    	Path excludedFile = newLocalDir.resolve("excludedFile");
    	Path excludedDir = newLocalDir.resolve("excludedDir");
    	Path excludedFile2 = excludedDir.resolve("excludedFile2");
    	
    	createDirectories(testingLocalDirectory, newLocalDir, excludedDir);
        createFiles(testingLocalDirectory, newLocalFile, excludedFile, excludedFile2, Path.of(".sync_exclude"));
        
        appendLineToFile(testingLocalDirectory.resolve(".sync_exclude"), "excludedFile");
        appendLineToFile(testingLocalDirectory.resolve(".sync_exclude"), "excludedDir");
    	
    	FileSynchronizer firstSync = testingFileSynchronizer(true, true, true);
        firstSync.synchronizeFileTrees();

        assertTrue(allFilesExist(testingRemoteDirectory, newLocalDir, newLocalFile));
        assertFalse(allFilesExist(testingRemoteDirectory, excludedDir));
        assertFalse(allFilesExist(testingRemoteDirectory, excludedFile));
        assertFalse(allFilesExist(testingRemoteDirectory, excludedFile2));
    }
}
