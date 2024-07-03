package fileSynchronizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;


public class FileSynchronizer {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    private final Set<Path> excludedPaths;
    private final long lastSyncMillis;
    private final FileSyncRoot localRoot, remoteRoot;
    private final boolean verbose;

    private enum fileSelection { FILES_ONLY, DIRECTORIES_ONLY }

    public FileSynchronizer(String localRootPath, String remoteRootPath, String localHostname, String remoteHostname, boolean verbose) {
        this.verbose = verbose;
        localRoot = new FileSyncRoot(localRootPath, remoteHostname);
        remoteRoot = new FileSyncRoot(remoteRootPath, localHostname);

        // Get excluded paths from both roots
        excludedPaths = localRoot.getExcludedPaths();
        excludedPaths.addAll(remoteRoot.getExcludedPaths());

        // Get last sync time
        lastSyncMillis = Math.max(localRoot.getLastSyncMillis(), remoteRoot.getLastSyncMillis());
    }

    public void synchronizeFileTrees() {
        // Compare file trees in a breadth-first fashion
        Set<Path> conflicts = syncFileTrees(Path.of(""));

        // Resolve conflicts manually
        Scanner conflictResponses = new Scanner(System.in);
        for (Path conflict : conflicts) {
            ZonedDateTime localModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(localRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());
            ZonedDateTime remoteModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(remoteRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());

            System.out.println("\nConflict:");
            System.out.println("\tLocal  '" + localRoot.resolve(conflict) + "' modified " + localModified.format(TIMESTAMP_FORMATTER));
            System.out.println("\tRemote '" + remoteRoot.resolve(conflict) + "' modified " + remoteModified.format(TIMESTAMP_FORMATTER));
            System.out.print("Take local changes, or remote? (l/r)");
            String decision = conflictResponses.nextLine();
            copy(conflict, decision.equals("l"));
        }
        conflictResponses.close();

        // Export excluded paths to .sync_exclude
        localRoot.writeExcludedPathsList(excludedPaths);
        remoteRoot.writeExcludedPathsList(excludedPaths);

        // Set new last sync records
        long newSyncTimeMillis = System.currentTimeMillis();
        localRoot.setLastSync(newSyncTimeMillis);
        remoteRoot.setLastSync(newSyncTimeMillis);
    }

    private void copy(Path relativeFilepath, boolean fromLocal) {
        final Path sourcePathAbsolute = (fromLocal) ? localRoot.resolve(relativeFilepath) : remoteRoot.resolve(relativeFilepath);
        final Path destPathAbsolute = (fromLocal) ? remoteRoot.resolve(relativeFilepath) : localRoot.resolve(relativeFilepath);

        if (verbose) System.out.println("Copying '" + relativeFilepath + "' to " + ((fromLocal) ? "remote" : "local"));
        try {
            if (sourcePathAbsolute.toFile().isDirectory()) Files.walkFileTree(sourcePathAbsolute, new FileCopier(sourcePathAbsolute, destPathAbsolute, true));
            else Files.copy(sourcePathAbsolute, destPathAbsolute, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("ERROR: Copying '" + sourcePathAbsolute + "' to '" + destPathAbsolute + "' failed. Exiting...");
            System.exit(1);
        }
    }

    private void delete(Path relativePath, boolean fromLocal) {
        final Path absolutePath = (fromLocal) ? localRoot.resolve(relativePath) : remoteRoot.resolve(relativePath);

        if (verbose) System.out.println("Deleting '" + relativePath + "' from " + ((fromLocal) ? "remote" : "local"));
        try {
            if (absolutePath.toFile().isDirectory()) Files.walkFileTree(absolutePath, new FileDeleter(true));
            else Files.delete(absolutePath);
        } catch (IOException ioE) {
            System.out.println("ERROR: Deleting '" + absolutePath + "' failed. Exiting...");
            System.exit(1);
        }
    }

    private long getFileCreationTime(Path absolutePath) {
        try {
            final BasicFileAttributes fileAttrs = Files.readAttributes(absolutePath, BasicFileAttributes.class);
            return fileAttrs.creationTime().toMillis();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not retrieve creation time of '" + absolutePath + "'. Exiting...");
            System.exit(1);
        }

        return -1;
    }

    private Set<Path> syncFileTrees(Path relativePath) {
        final File localFile = localRoot.resolve(relativePath).toFile();
        final File remoteFile = remoteRoot.resolve(relativePath).toFile();

        final boolean localExists = localFile.exists();
        final boolean remoteExists = remoteFile.exists();

        Set<Path> conflicts = new HashSet<>();

        if (localExists && remoteExists) {
            if (localFile.isDirectory() && remoteFile.isDirectory()) {
                // Sync child files first
                for (Path filename : uniqueNonExcludedChildNames(localFile, remoteFile, fileSelection.FILES_ONLY)) {
                    conflicts.addAll(syncFileTrees(relativePath.resolve(filename)));
                }

                // Then recursively sync child directories
                for (Path dirname : uniqueNonExcludedChildNames(localFile, remoteFile, fileSelection.DIRECTORIES_ONLY)) {
                    conflicts.addAll(syncFileTrees(relativePath.resolve(dirname)));
                }
            }
            else if (localFile.isFile() && remoteFile.isFile()) {
                final long localModified = localFile.lastModified();
                final long remoteModified = remoteFile.lastModified();

                if (localModified > lastSyncMillis && remoteModified > lastSyncMillis) return Set.of(relativePath); // Case: both files modified since last sync. Conflict
                else if (Math.max(localModified, remoteModified) > lastSyncMillis) copy(relativePath, localModified > lastSyncMillis); // Case: One and only one file has been modified since last sync. Copy it to opposite file tree
            }
        }
        else if (localExists || remoteExists) {
            if (getFileCreationTime((localExists) ? localFile.toPath() : remoteFile.toPath()) > lastSyncMillis) copy(relativePath, localExists);
            else delete(relativePath, localExists);
        }

        return conflicts;
    }

    private boolean isExcludedPath(Path candidate) {
        return excludedPaths.contains(candidate);
    }

    private Set<Path> uniqueNonExcludedChildNames(File localDir, File remoteDir, fileSelection fileTypeToList) {
        Set<Path> uniquePaths = new HashSet<>();
        uniquePaths.addAll(Arrays.stream(remoteDir.listFiles(f -> (fileTypeToList == fileSelection.FILES_ONLY) ? f.isFile() : f.isDirectory())).map(File::toPath).collect(Collectors.toSet()));
        uniquePaths.addAll(Arrays.stream(localDir.listFiles(f -> (fileTypeToList == fileSelection.FILES_ONLY) ? f.isFile() : f.isDirectory())).map(File::toPath).collect(Collectors.toSet()));

        return uniquePaths.stream().filter(p -> !isExcludedPath(p)).map(Path::getFileName).collect(Collectors.toSet());
    }

}