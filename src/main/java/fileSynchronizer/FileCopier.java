package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public class FileCopier extends SimpleFileVisitor<Path> {

    private final Path destinationDir, sourceDir;
    private final Set<Path> excludedPaths;
    private final boolean verbose;

    public FileCopier(Path sourceDir, Path destinationDir, Set<Path> excludedPaths, boolean verbose) {
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
        this.excludedPaths = excludedPaths;
        this.verbose = verbose;
    }

    private boolean isExcludedPath(Path candidate) {
        for (Path excluded : excludedPaths) {
            if (candidate.endsWith(excluded)) return true;
        }

        return false;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (isExcludedPath(path)) {
            if (verbose) System.out.println("SKIP: Excluded path '" + path + "'");
            return FileVisitResult.SKIP_SUBTREE;
        }

        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        if (isExcludedPath(path)) {
            if (verbose) System.out.println("SKIP: Excluded path '" + path + "'");
            return FileVisitResult.CONTINUE;
        }

        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Copying failed: '" + path + "' -> '" + destinationDir.resolve(sourceDir.relativize(path)) + "'");
        return FileVisitResult.CONTINUE;
    }
}
