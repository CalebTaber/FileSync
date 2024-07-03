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
    private static Set<Path> excludedPaths;

    private enum fileSelection { FILES_ONLY, DIRECTORIES_ONLY }

    private final long LAST_SYNC_MILLIS;
    private final FileSyncRoot LOCAL_ROOT, REMOTE_ROOT;

    public FileSynchronizer(Path localRootPath, Path remoteRootPath, String localHostname, String remoteHostname) {
        LOCAL_ROOT = new FileSyncRoot(localRootPath, localHostname);
        REMOTE_ROOT = new FileSyncRoot(remoteRootPath, remoteHostname);

        // Get excluded paths from both roots
        excludedPaths = LOCAL_ROOT.excludedPaths;
        excludedPaths.addAll(REMOTE_ROOT.excludedPaths);

        // Get last sync time
        LAST_SYNC_MILLIS = LOCAL_ROOT.LAST_SYNC_MILLIS;
    }

    public void synchronizeFileTrees() {
        // Compare file trees in a breadth-first fashion
        Set<Path> conflicts = syncFileTrees(Path.of(""));

        // Resolve conflicts manually
        Scanner conflictResponses = new Scanner(System.in);
        for (Path conflict : conflicts) {
            ZonedDateTime localModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(LOCAL_ROOT.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());
            ZonedDateTime remoteModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(REMOTE_ROOT.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());

            System.out.println("\nConflict:");
            System.out.println("\tLocal  '" + LOCAL_ROOT.resolve(conflict) + "' modified " + localModified.format(TIMESTAMP_FORMATTER));
            System.out.println("\tRemote '" + REMOTE_ROOT.resolve(conflict) + "' modified " + remoteModified.format(TIMESTAMP_FORMATTER));
            System.out.print("Take local changes, or remote? (l/r)");
            String decision = conflictResponses.nextLine();
            copy(conflict, decision.equals("l"));
        }
        conflictResponses.close();

        // Export excluded paths to .sync_exclude
        LOCAL_ROOT.writeExcludedPathsList(excludedPaths);
        REMOTE_ROOT.writeExcludedPathsList(excludedPaths);

        // Set new last sync records
        long newSyncTimeMillis = System.currentTimeMillis();
        LOCAL_ROOT.setLastSync(newSyncTimeMillis);
        REMOTE_ROOT.setLastSync(newSyncTimeMillis);
    }

    private void copy(Path relativeFilepath, boolean fromLocal) {
        final Path source = (fromLocal) ? LOCAL_ROOT.resolve(relativeFilepath) : REMOTE_ROOT.resolve(relativeFilepath);
        final Path destination = (fromLocal) ? REMOTE_ROOT.resolve(relativeFilepath) : LOCAL_ROOT.resolve(relativeFilepath);

        System.out.println("Copying '" + relativeFilepath + "' to " + ((fromLocal) ? "remote" : "local"));
        FileTreeUtils.copy(source, destination);
    }

    private void delete(Path relativeFilePath, boolean fromLocal) {
        final Path absoluteFilepath = (fromLocal) ? LOCAL_ROOT.resolve(relativeFilePath) : REMOTE_ROOT.resolve(relativeFilePath);

        System.out.println("Deleting '" + relativeFilePath + "' from " + ((fromLocal) ? "remote" : "local"));
        FileTreeUtils.delete(absoluteFilepath);
    }

    private long getCreateTime(Path absolutePath) {
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
        final File localFile = LOCAL_ROOT.resolve(relativePath).toFile();
        final File remoteFile = REMOTE_ROOT.resolve(relativePath).toFile();

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

                if (localModified > LAST_SYNC_MILLIS && remoteModified > LAST_SYNC_MILLIS) return Set.of(relativePath); // Case: both files modified since last sync. Conflict
                else if (Math.max(localModified, remoteModified) > LAST_SYNC_MILLIS) copy(relativePath, localModified > LAST_SYNC_MILLIS); // Case: One and only one file has been modified since last sync. Copy it to opposite file tree
            }
        }
        else if (localExists || remoteExists) {
            if (getCreateTime((localExists) ? localFile.toPath() : remoteFile.toPath()) > LAST_SYNC_MILLIS) copy(relativePath, localExists);
            else delete(relativePath, localExists);
        }

        return conflicts;
    }

    private boolean isExcludedPath(Path candidate) {
        for (Path excludedEnding : excludedPaths) {
            if (candidate.endsWith(excludedEnding)) return true;
        }

        return false;
    }

    private Set<Path> uniqueNonExcludedChildNames(File localDir, File remoteDir, fileSelection fileTypeToList) {
        Set<Path> uniquePaths = new HashSet<>();
        uniquePaths.addAll(Arrays.stream(remoteDir.listFiles(f -> (fileTypeToList == fileSelection.FILES_ONLY) ? f.isFile() : f.isDirectory())).map(File::toPath).collect(Collectors.toSet()));
        uniquePaths.addAll(Arrays.stream(localDir.listFiles(f -> (fileTypeToList == fileSelection.FILES_ONLY) ? f.isFile() : f.isDirectory())).map(File::toPath).collect(Collectors.toSet()));

        return uniquePaths.stream().filter(p -> !isExcludedPath(p)).map(Path::getFileName).collect(Collectors.toSet());
    }

}