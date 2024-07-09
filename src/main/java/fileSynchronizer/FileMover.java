package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMover extends SimpleFileVisitor<Path> {

    private final boolean verbose;
    private final Path sourceDirAbsolute, destinationDirAbsolute;

    public FileMover(Path sourceDirAbsolute, Path destinationDirAbsolute, boolean verbose) {
        this.verbose = verbose;
        this.sourceDirAbsolute = sourceDirAbsolute;
        this.destinationDirAbsolute = destinationDirAbsolute;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.move(path, destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("MOVE: " + path + " -> " + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.move(path, destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("MOVE: " + path + " -> " + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.out.println("ERROR: Moving file '" + path + "' to '" + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)) + "' failed. Exiting...");
        System.exit(1);
        return FileVisitResult.TERMINATE;
    }

}
