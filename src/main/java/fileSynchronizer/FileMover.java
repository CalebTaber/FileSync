package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMover extends SimpleFileVisitor<Path> {

    private final boolean verbose;
    private final Path sourceDir, destinationDir;

    public FileMover(Path sourceDir, Path destinationDir, boolean verbose) {
        this.verbose = verbose;
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.COPY_ATTRIBUTES);
        if (verbose) System.out.println("MOVE: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Path absoluteDestination = destinationDir.resolve(sourceDir.relativize(path));
        if (path.equals(sourceDir)) absoluteDestination = absoluteDestination.resolve(path.getFileName());

        Files.move(path, absoluteDestination);
        if (verbose) System.out.println("MOVE: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Files.delete(path);
        if (verbose) System.out.println("MOVE: " + path + " -> " + destinationDir.resolve(sourceDir.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Move failed: '" + path + "' -> '" + destinationDir.resolve(sourceDir.relativize(path)) + "'");
        return FileVisitResult.CONTINUE;
    }

}
