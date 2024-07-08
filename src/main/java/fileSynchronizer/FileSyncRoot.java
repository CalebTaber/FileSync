package fileSynchronizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class FileSyncRoot {
    private final Path root, syncTrash;
    private final File syncExclude, syncLog;
    private final long lastSyncMillis;
    private final String remoteNickname;
    private final Set<Path> excludedPaths;

    public FileSyncRoot(String rootPath, String remoteNickname) {
        root = Path.of(rootPath);

        syncExclude = root.resolve(".sync_exclude").toFile();
        syncLog = root.resolve(".sync_log").toFile();
        syncTrash = root.resolve(".sync_trash");

        try {
            if (syncTrash.toFile().exists()) {
                // Delete any pre-existing trashed files before creating the directory again
                Files.walkFileTree(syncTrash, new FileDeleter(false));
            }
            Files.createDirectory(syncTrash);
        } catch (IOException ioE) {}

        this.remoteNickname = remoteNickname;
        lastSyncMillis = getLastSync();

        excludedPaths = readExcludedPathsList();
        excludedPaths.add(Path.of(".sync_exclude"));
        excludedPaths.add(Path.of(".sync_log"));
        excludedPaths.add(Path.of(".sync_trash"));
    }

    public long getLastSyncMillis() {
        return lastSyncMillis;
    }

    public Path resolve(Path p) {
        return root.resolve(p);
    }

    public Path getSyncTrash() {
        return syncTrash;
    }

    public Set<Path> getExcludedPaths() {
        return excludedPaths;
    }

    public String getRemoteNickname() {
        return remoteNickname;
    }

    public void writeExcludedPathsList(Set<Path> excludedPaths) {
        try {
            syncExclude.createNewFile();

            FileWriter exclusionWriter = new FileWriter(syncExclude);
            for (Path excluded : excludedPaths) {
                exclusionWriter.write(excluded.toString());
                exclusionWriter.write("\n");
            }
            exclusionWriter.close();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not create file '" + syncExclude.toPath() + "'. Exiting...");
            System.exit(1);
        }
    }

    public void setLastSync(long newLastSyncMillis) {
        List<String> syncRecords = new ArrayList<>();
        syncRecords.add(remoteNickname + "," + newLastSyncMillis);

        try {
            if (syncLog.createNewFile()) {
                Scanner logReader = new Scanner(syncLog);

                while (logReader.hasNext()) {
                    String hostNameAndLastSync = logReader.nextLine();

                    if (!hostNameAndLastSync.startsWith(remoteNickname + ",")) syncRecords.add(hostNameAndLastSync);
                }
                logReader.close();
            }

            FileWriter logWriter = new FileWriter(syncLog);
            for (String syncRecord : syncRecords) {
                logWriter.write(syncRecord);
            }
            logWriter.close();

        } catch (IOException ioE) {
            System.out.println();
        }
    }

    private Set<Path> readExcludedPathsList() {
        Set<Path> excludedPaths = new HashSet<>();

        try {
            if (syncExclude.createNewFile()) return excludedPaths;

            Scanner exclusionReader = new Scanner(syncExclude);
            while (exclusionReader.hasNext()) {
                excludedPaths.add(Path.of(exclusionReader.nextLine()));
            }
            exclusionReader.close();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not read file '" + syncExclude.toPath() + "'. Exiting...");
            System.exit(1);
        }

        return excludedPaths;
    }

    private long getLastSync() {
        try {
            Scanner logReader = new Scanner(syncLog);
            while (logReader.hasNext()) {
                String[] hostNameAndLastSync = logReader.nextLine().split(",");
                if (hostNameAndLastSync[0].equals(remoteNickname)) return Long.parseLong(hostNameAndLastSync[1]);
            }
            logReader.close();
        } catch (IOException ioE) {
            // If the file can't be opened, it's likely because the file doesn't exist, meaning there is no sync history
            // So return 0, that way all the files between the two directories are synced
            return 0;
        }

        return 0;
    }
}