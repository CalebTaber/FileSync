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

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    private final Set<Path> excludedPaths;
    private final long lastSyncMillis;
    private final FileSyncRoot localRoot, remoteRoot;
    private final boolean verbose;

    public FileSynchronizer(String localRootPath, String remoteRootPath, String localHostname, String remoteHostname, boolean verbose) {
        this.verbose = verbose;
        localRoot = new FileSyncRoot(localRootPath, remoteHostname);
        remoteRoot = new FileSyncRoot(remoteRootPath, localHostname);

        // Merge excluded paths from both roots
        excludedPaths = localRoot.getExcludedPaths();
        excludedPaths.addAll(remoteRoot.getExcludedPaths());

        // Get last sync time
        lastSyncMillis = Math.max(localRoot.getLastSyncMillis(), remoteRoot.getLastSyncMillis());
    }

    public void synchronizeFileTrees() {
        // Compare file trees in a breadth-first fashion and collect paths of conflicting files
        Set<Path> conflicts = syncFileTrees(Path.of(""));

        // Resolve conflicts manually
        for (Path conflict : conflicts) {
            ZonedDateTime localModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(localRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());
            ZonedDateTime remoteModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(remoteRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());

            System.out.println("\nConflict:");
            System.out.println("\tLocal  '" + localRoot.resolve(conflict) + "' modified " + localModified.format(timestampFormatter));
            System.out.println("\tRemote '" + remoteRoot.resolve(conflict) + "' modified " + remoteModified.format(timestampFormatter));
            System.out.print("Take local changes, or remote? (l/r): ");
            String decision = Driver.USER_INPUT.nextLine();

            trashOldAndCopyNewOver(conflict, decision.equals("l"));
        }

        // Print all trashed file names and ask user if they want to delete them or not
        if (localRoot.getSyncTrash().toFile().list().length != 0 || remoteRoot.getSyncTrash().toFile().list().length != 0) {
            System.out.println("\nAll trashed files:");

            try {
                Files.walkFileTree(localRoot.getSyncTrash(), new FileNamePrinter(localRoot.getSyncTrash(), remoteRoot.getRemoteNickname() + ": "));
                Files.walkFileTree(remoteRoot.getSyncTrash(), new FileNamePrinter(remoteRoot.getSyncTrash(), localRoot.getRemoteNickname() + ": "));
            } catch (IOException ioE) {
                System.out.println("Could not print all trashed file names");
            }

            System.out.print("Delete all trashed files? (y/n): ");
            String deleteDecision = Driver.USER_INPUT.nextLine();

            if (deleteDecision.equalsIgnoreCase("y")) {
                deleteFile(localRoot.getSyncTrash());
                deleteFile(remoteRoot.getSyncTrash());
            } else System.out.println("No trashed files will be deleted");
        }

        // Export excluded paths to .sync_exclude
        localRoot.writeExcludedPathsList(excludedPaths);
        remoteRoot.writeExcludedPathsList(excludedPaths);

        // Set new last sync records
        long newSyncTimeMillis = System.currentTimeMillis();
        localRoot.setLastSync(newSyncTimeMillis);
        remoteRoot.setLastSync(newSyncTimeMillis);
    }

    private void trashOldAndCopyNewOver(Path relativeFilepath, boolean fromLocal) {
        Path source = ((fromLocal) ? localRoot : remoteRoot).resolve(relativeFilepath);
        Path destination = ((fromLocal) ? remoteRoot : localRoot).resolve(relativeFilepath);
        Path trash = ((fromLocal) ? remoteRoot : localRoot).getSyncTrash();

        copyFile(destination, trash.resolve(relativeFilepath));
        copyFile(source, destination);
    }

    private void trashOldAndDelete(Path relativeFilepath, boolean trashLocalFile) {
        Path absolute = ((trashLocalFile) ? localRoot : remoteRoot).resolve(relativeFilepath);
        Path trash = ((trashLocalFile) ? localRoot : remoteRoot).getSyncTrash();

        copyFile(absolute, trash.resolve(relativeFilepath));
        deleteFile(absolute);
    }

    private void copyFile(Path source, Path destination) {
        try {
            if (source.toFile().isDirectory()) Files.walkFileTree(source, new FileCopier(source, destination, verbose));
            else {
                if (verbose) System.out.println("Copying '" + source + "' to " + destination);
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("ERROR: Copying '" + source + "' to '" + destination + "' failed. Exiting...");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void deleteFile(Path filepath) {
        try {
            if (filepath.toFile().isDirectory()) Files.walkFileTree(filepath, new FileDeleter(verbose));
            else {
                if (verbose) System.out.println("Deleting '" + filepath + "'");
                Files.delete(filepath);
            }
        } catch (IOException ioE) {
            System.out.println("ERROR: Deleting '" + filepath + "' failed. Exiting...");
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
                for (Path filename : uniqueNonExcludedChildNames(localFile, remoteFile)) {
                    conflicts.addAll(syncFileTrees(relativePath.resolve(filename)));
                }
            }
            else if (localFile.isFile() && remoteFile.isFile()) {
                final long localModified = localFile.lastModified();
                final long remoteModified = remoteFile.lastModified();

                if (localModified > lastSyncMillis && remoteModified > lastSyncMillis) return Set.of(relativePath); // Case: both files modified since last sync. Conflict
                else if (Math.max(localModified, remoteModified) > lastSyncMillis) trashOldAndCopyNewOver(relativePath, localModified > lastSyncMillis);
            }
        }
        else if (localExists || remoteExists) {
            if (getFileCreationTime((localExists) ? localFile.toPath() : remoteFile.toPath()) > lastSyncMillis) {
                copyFile(((localExists) ? localRoot : remoteRoot).resolve(relativePath), ((localExists) ? remoteRoot : localRoot).resolve(relativePath));
            }
            else trashOldAndDelete(relativePath, localExists);
        }

        return conflicts;
    }

    private boolean isExcludedPath(Path candidate) {
        for (Path excluded : excludedPaths) {
            if (candidate.endsWith(excluded)) return true;
        }

        return false;
    }

    private Set<Path> uniqueNonExcludedChildNames(File localDir, File remoteDir) {
        Set<Path> uniquePaths = new HashSet<>();

        uniquePaths.addAll(Arrays.stream(localDir.listFiles()).map(File::toPath).collect(Collectors.toSet()));
        uniquePaths.addAll(Arrays.stream(remoteDir.listFiles()).map(File::toPath).collect(Collectors.toSet()));

        return uniquePaths.stream().filter(p -> !isExcludedPath(p)).map(Path::getFileName).collect(Collectors.toSet());
    }

}