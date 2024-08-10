package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMover extends SimpleFileVisitor<Path> {

    private final boolean verbose;
    private final Path sourceDir, destinationDir, sourceRoot;
    private final String sourceNickname, destinationNickname;

    public FileMover(Path sourceDir, Path destinationDir, String sourceNickname, String destinationNickname, Path sourceRoot, boolean verbose) {
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
        this.sourceNickname = sourceNickname;
        this.destinationNickname = destinationNickname;
        this.sourceRoot = sourceRoot;
        this.verbose = verbose;
    }

    private void logMove(Path filePath) {
        if (!verbose) return;

        System.out.println("MOVE: " + sourceNickname + " '" + sourceRoot.relativize(filePath) + "' to " + destinationNickname + " '" + sourceRoot.relativize(filePath) + "'");
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDir.resolve(sourceDir.relativize(path)), StandardCopyOption.COPY_ATTRIBUTES);
        logMove(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Path absoluteDestination = destinationDir.resolve(sourceDir.relativize(path));

        Files.move(path, absoluteDestination);
        logMove(path);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Files.delete(path);
        logMove(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Move failed: '" + path + "' -> '" + destinationDir.resolve(sourceDir.relativize(path)) + "'");
        return FileVisitResult.CONTINUE;
    }

}
