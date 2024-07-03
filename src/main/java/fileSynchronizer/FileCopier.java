package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileCopier extends SimpleFileVisitor<Path> {

    private final Path destinationDirAbsolute, sourceDirAbsolute;
    private final boolean verbose;

    public FileCopier(Path absoluteDirSource, Path destinationDirAbsolute, boolean verbose) {
        this.sourceDirAbsolute = absoluteDirSource;
        this.destinationDirAbsolute = destinationDirAbsolute;
        this.verbose = verbose;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        Files.copy(path, destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
        if (verbose) System.out.println("COPY: " + path + " -> " + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.out.println("ERROR: Copying file '" + path + "' to '" + destinationDirAbsolute.resolve(sourceDirAbsolute.relativize(path)) + "' failed. Exiting...");
        System.exit(1);
        return FileVisitResult.TERMINATE;
    }
}
