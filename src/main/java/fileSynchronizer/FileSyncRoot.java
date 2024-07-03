import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class FileSyncRoot {
    private final Path ROOT;
    private final File SYNC_EXCLUDE, SYNC_LOG;
    public final long LAST_SYNC_MILLIS;
    public final String HOSTNAME;

    public Set<Path> excludedPaths;

    public FileSyncRoot(Path root, String hostname) {
        ROOT = root;
        SYNC_EXCLUDE = root.resolve(".sync_exclude").toFile();
        SYNC_LOG = root.resolve(".sync_log").toFile();

        HOSTNAME = hostname;
        LAST_SYNC_MILLIS = getLastSync();

        excludedPaths = readExcludedPathsList();
    }

    public void writeExcludedPathsList(Set<Path> excludedPaths) {
        try {
            SYNC_EXCLUDE.createNewFile();

            FileWriter exclusionWriter = new FileWriter(SYNC_EXCLUDE);
            for (Path excluded : excludedPaths) {
                exclusionWriter.write(excluded.toString());
            }
            exclusionWriter.close();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not create file '" + SYNC_EXCLUDE.toPath() + "'. Exiting...");
            System.exit(1);
        }
    }

    public void setLastSync(long newLastSyncMillis) {
        List<String> syncRecords = new ArrayList<>();
        syncRecords.add(HOSTNAME + "," + newLastSyncMillis);

        try {
            if (SYNC_LOG.createNewFile()) {
                Scanner logReader = new Scanner(SYNC_LOG);

                while (logReader.hasNext()) {
                    String hostNameAndLastSync = logReader.nextLine();

                    if (!hostNameAndLastSync.startsWith(HOSTNAME + ",")) syncRecords.add(hostNameAndLastSync);
                }
                logReader.close();
            }

            FileWriter logWriter = new FileWriter(SYNC_LOG);
            for (String syncRecord : syncRecords) {
                logWriter.write(syncRecord);
            }
            logWriter.close();

        } catch (IOException ioE) {
            System.out.println();
        }
    }

    public Path resolve(Path p) {
        return ROOT.resolve(p);
    }

    public Set<Path> readExcludedPathsList() {
        Set<Path> excludedPaths = new HashSet<>();

        try {
            if (SYNC_EXCLUDE.createNewFile()) return excludedPaths;

            Scanner exclusionReader = new Scanner(SYNC_EXCLUDE);
            while (exclusionReader.hasNext()) {
                excludedPaths.add(Path.of(exclusionReader.nextLine()));
            }
            exclusionReader.close();
        } catch (IOException ioE) {
            System.out.println("ERROR: Could not read file '" + SYNC_EXCLUDE.toPath() + "'. Exiting...");
            System.exit(1);
        }

        return excludedPaths;
    }

    private long getLastSync() {
        try {
            Scanner logReader = new Scanner(SYNC_LOG);
            while (logReader.hasNext()) {
                String[] hostNameAndLastSync = logReader.nextLine().split(",");
                if (hostNameAndLastSync[0].equals(HOSTNAME)) return Long.parseLong(hostNameAndLastSync[1]);
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