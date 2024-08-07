package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileNamePrinter extends SimpleFileVisitor<Path> {

    private final Path root;
    private final String prefix;

    public FileNamePrinter(Path root, String prefix) {
        this.root = root;
        this.prefix = prefix;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        System.out.println(prefix + root.relativize(path));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) {
        System.err.println("ERROR: Printing file '" + path + "' failed. Exiting...");
        System.exit(1);
        return FileVisitResult.TERMINATE;
    }

}
