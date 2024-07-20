package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileCopier extends SimpleFileVisitor<Path> {

    private final Path destinationDir, sourceDir;
    private final boolean verbose;

    public FileCopier(Path sourceDir, Path destinationDir, boolean verbose) {
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
        this.verbose = verbose;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Copying file '" + path + "' to '" + destinationDir.resolve(sourceDir.relativize(path)) + "' failed. Exiting...");
        System.exit(1);
        return FileVisitResult.TERMINATE;
    }
}
