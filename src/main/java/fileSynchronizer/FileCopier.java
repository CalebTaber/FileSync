package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public class FileCopier extends SimpleFileVisitor<Path> {

    private final Path destinationDir, sourceDir;
    private final Set<Path> excludedPaths;
    private final boolean verbose;
    private final String sourceName, destinationName;

    public FileCopier(Path sourceDir, Path destinationDir, Set<Path> excludedPaths, String sourceName, String destinationName, boolean verbose) {
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
        this.excludedPaths = excludedPaths;
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.verbose = verbose;
    }

    private boolean isExcludedPath(Path candidate) {
        for (Path excluded : excludedPaths) {
            if (candidate.endsWith(excluded)) return true;
        }

        return false;
    }

    private void logCopy(Path pathToCopy) {
        if (!verbose) return;
        
        Path filePath = (pathToCopy.equals(sourceDir)) ? pathToCopy.getFileName() : sourceDir.relativize(pathToCopy);
        System.out.println("COPY: " + filePath + " from " + sourceName + " to " + destinationName);
    }
    
    private void logSkip(Path pathToSkip) {
        if (!verbose) return;
        
        Path filePath = (pathToSkip.equals(sourceDir)) ? pathToSkip.getFileName() : sourceDir.relativize(pathToSkip);
        System.out.println("SKIP: Excluded path '" + filePath + "'");
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (isExcludedPath(path)) {
            logSkip(path);
            return FileVisitResult.SKIP_SUBTREE;
        }

        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        logCopy(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (isExcludedPath(path)) {
            logSkip(path);
            return FileVisitResult.CONTINUE;
        }

        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        logCopy(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Copying failed: '" + path + "' -> '" + destinationDir.resolve(sourceDir.relativize(path)) + "'");
        return FileVisitResult.CONTINUE;
    }
}
