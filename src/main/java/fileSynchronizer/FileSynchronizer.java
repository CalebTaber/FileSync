package fileSynchronizer;

import java.io.*;
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
    private final BufferedReader userInputReader;

    public FileSynchronizer(String localRootPath, String remoteRootPath, String localNickname, String remoteNickname, InputStream userInput, boolean verbose) {
        this.verbose = verbose;
        userInputReader = new BufferedReader(new InputStreamReader(userInput));

        localRoot = new FileSyncRoot(localRootPath, localNickname, remoteNickname, verbose);
        remoteRoot = new FileSyncRoot(remoteRootPath, remoteNickname, localNickname, verbose);

        // Merge excluded paths from both roots
        excludedPaths = localRoot.getExcludedPaths();
        excludedPaths.addAll(remoteRoot.getExcludedPaths());

        localRoot.setExcludedPaths(excludedPaths);
        remoteRoot.setExcludedPaths(excludedPaths);

        // Get last sync time
        lastSyncMillis = Math.max(localRoot.getLastSyncMillis(), remoteRoot.getLastSyncMillis());
    }

    public void synchronizeFileTrees() {
        if (lastSyncMillis == 0) {
            System.out.println("No sync record exists between " + localRoot.getNickname() + " and " + remoteRoot.getNickname() + ". What would you like to do?");
            System.out.println("\t(1) Perform full sync now");
            System.out.println("\t(2) Both directories are already equivalent, so just add a sync record");
            String response = getUserInput();

            if (response.equalsIgnoreCase("1")) ;
            else if (response.equalsIgnoreCase("2")) {
               closeRoots();
               return;
            }
            else System.exit(1);
        }

        // Compare file trees in a breadth-first fashion and collect paths of conflicting files
        Set<Path> conflicts = syncFileTrees(Path.of(""));

        // Resolve conflicts manually
        for (Path conflict : conflicts) {
            ZonedDateTime localModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(localRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());
            ZonedDateTime remoteModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(remoteRoot.resolve(conflict).toFile().lastModified()), ZoneId.systemDefault());

            System.out.println(System.lineSeparator() + "Conflict:");
            System.out.println("\t" + localRoot.getNickname() + " (1): '" + conflict + "' modified " + localModified.format(timestampFormatter));
            System.out.println("\t" + remoteRoot.getNickname() + " (2): '" + conflict + "' modified " + remoteModified.format(timestampFormatter));
            System.out.print("Take changes from " + localRoot.getNickname() + " (1) or from " + remoteRoot.getNickname() + " (2)?: ");
            String decision = getUserInput();
            boolean takeLocal = decision.equalsIgnoreCase("1");

            if (takeLocal) remoteRoot.copyFromRemote(conflict, localRoot.getRoot());
            else localRoot.copyFromRemote(conflict, remoteRoot.getRoot());
        }

        // Print all trashed file names and ask user if they want to delete them or not
        if (localRoot.getSyncTrash().toFile().list().length != 0 || remoteRoot.getSyncTrash().toFile().list().length != 0) {
            System.out.println(System.lineSeparator() + "All trashed files:");

            try {
                Files.walkFileTree(localRoot.getSyncTrash(), new FileNamePrinter(localRoot.getSyncTrash(), localRoot.getNickname() + ": "));
                Files.walkFileTree(remoteRoot.getSyncTrash(), new FileNamePrinter(remoteRoot.getSyncTrash(), remoteRoot.getNickname() + ": "));
            } catch (IOException ioE) {
                System.out.println("Could not print all trashed file names");
            }

            System.out.print("Delete all trashed files? (y/n): ");
            String deleteDecision = getUserInput();

            if (deleteDecision.equalsIgnoreCase("y")) {
                localRoot.clearTrash();
                remoteRoot.clearTrash();
            }
            else System.out.println("No trashed files will be deleted");
        }

        closeRoots();
    }

    private void closeRoots() {
        // Export excluded paths to .sync_exclude
        localRoot.writeExcludedPathsList();
        remoteRoot.writeExcludedPathsList();

        // Set new last sync records
        long newSyncTimeMillis = System.currentTimeMillis();
        localRoot.setLastSync(newSyncTimeMillis);
        remoteRoot.setLastSync(newSyncTimeMillis);
    }

    private long getFileCreationTime(Path absolutePath) {
        try {
            BasicFileAttributes fileAttrs = Files.readAttributes(absolutePath, BasicFileAttributes.class);
            return fileAttrs.creationTime().toMillis();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not get file creation time of '" + absolutePath + "'");
            // If the file creation time cannot be ascertained, err on the side of caution and return max long value so no files are erroneously deleted
            return Long.MAX_VALUE;
        }
    }

    private Set<Path> syncFileTrees(Path relativePath) {
        File localFile = localRoot.resolve(relativePath).toFile();
        File remoteFile = remoteRoot.resolve(relativePath).toFile();

        boolean localExists = localFile.exists();
        boolean remoteExists = remoteFile.exists();

        Set<Path> conflicts = new HashSet<>();

        if (localExists && remoteExists) {
            if (localFile.isDirectory() && remoteFile.isDirectory()) {
                for (Path filename : uniqueNonExcludedChildNames(localFile, remoteFile)) {
                    conflicts.addAll(syncFileTrees(relativePath.resolve(filename)));
                }
            }
            else if (localFile.isFile() && remoteFile.isFile()) {
                long localModified = localFile.lastModified();
                long remoteModified = remoteFile.lastModified();

                if (localModified > lastSyncMillis && remoteModified > lastSyncMillis) return Set.of(relativePath); // Case: both files modified since last sync. Conflict
                else if (localModified > lastSyncMillis) remoteRoot.copyFromRemote(relativePath, localRoot.getRoot());
                else if (remoteModified > lastSyncMillis) localRoot.copyFromRemote(relativePath, remoteRoot.getRoot());
            }
            else {
                System.err.println("ERROR: '" +  localFile.getPath() + "' AND '" + remoteFile.getPath() + "' are not the same type. Exiting...");
                System.exit(1);
            }
        }
        else if (localExists && getFileCreationTime(localFile.toPath()) > lastSyncMillis) remoteRoot.copyFromRemote(relativePath, localRoot.getRoot());
        else if (remoteExists && getFileCreationTime(remoteFile.toPath()) > lastSyncMillis) localRoot.copyFromRemote(relativePath, remoteRoot.getRoot());
        else if (!localExists) remoteRoot.trash(relativePath);
        else localRoot.trash(relativePath);

        return conflicts;
    }

    private String getUserInput() {
        try {
            return userInputReader.readLine();
        } catch (IOException e) {
            System.err.println("ERROR: NO INPUT READABLE");
            System.exit(1);
        }

        return "";
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