package fileSynchronizer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileDeleter extends SimpleFileVisitor<Path> {

    private final boolean verbose;

    public FileDeleter(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.delete(path);
        if (verbose) System.out.println("DELETE: " + path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.out.println("ERROR: Deletion of file '" + path + "' failed. Exiting...");
        System.exit(1);
        return FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Files.delete(path);
        if (verbose) System.out.println("DELETE: " + path);
        return FileVisitResult.CONTINUE;
    }

}
