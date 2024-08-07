package fileSynchronizer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileDeleter extends SimpleFileVisitor<Path> {

    private final boolean verbose;
    private final String rootNickname;
    private final Path rootPath;

    public FileDeleter(String rootNickname, Path rootPath, boolean verbose) {
        this.rootNickname = rootNickname;
        this.rootPath = rootPath;
        this.verbose = verbose;
    }

    private void logDeletion(Path filePath) {
        if (verbose) System.out.println("DELETE: " + rootNickname + " '" + rootPath.relativize(filePath) + "'");
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.delete(path);
        logDeletion(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Files.delete(path);
        logDeletion(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Deletion failed: '" + path + "'");
        return FileVisitResult.CONTINUE;
    }

}
