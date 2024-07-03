package fileSynchronizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class FileSyncRoot {
    private final Path root;
    private final File syncExclude, syncLog, syncTrash;
    private final long lastSyncMillis;
    private final String remoteNickname;
    private final Set<Path> excludedPaths;

    public FileSyncRoot(Path root, String hostname) {
        this.root = root;
        syncExclude = root.resolve(".sync_exclude").toFile();
        syncLog = root.resolve(".sync_log").toFile();
        syncTrash = root.resolve(".sync_trash").toFile();

        try {
            Files.createDirectory(syncTrash.toPath());
        } catch (IOException ioE) {
            System.out.println("Sync trash could not be created at '" + syncTrash.toPath() + "'. Exiting...");
            System.exit(1);
        }

        remoteNickname = hostname;
        lastSyncMillis = getLastSync();

        excludedPaths = readExcludedPathsList();
    }

    public long getLastSyncMillis() {
        return lastSyncMillis;
    }

    public Path resolve(Path p) {
        return root.resolve(p);
    }

    public Set<Path> getExcludedPaths() {
        return excludedPaths;
    }

    public void writeExcludedPathsList(Set<Path> excludedPaths) {
        try {
            syncExclude.createNewFile();

            FileWriter exclusionWriter = new FileWriter(syncExclude);
            for (Path excluded : excludedPaths) {
                exclusionWriter.write(excluded.toString());
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