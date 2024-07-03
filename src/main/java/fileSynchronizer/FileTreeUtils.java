package fileSynchronizer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public final class FileTreeUtils {

    public static void recursivelyDeleteDirectory(Path absoluteDirPath) {
        try {
            Files.walkFileTree(absoluteDirPath, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    System.out.println("DELETE: " + path);
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
                    System.out.println("DELETE: " + path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("ERROR: Deleting directory '" + absoluteDirPath + "' failed. Exiting...");
            System.exit(1);
        }
    }

    public static void delete(Path absolutePath) {
        if (absolutePath.toFile().isDirectory()) recursivelyDeleteDirectory(absolutePath);
        else {
            try {
                Files.delete(absolutePath);
            } catch (IOException ioE) {
                System.out.println("ERROR: Deleting directory '" + absolutePath + "' failed. Exiting...");
                System.exit(1);
            }
        }
    }

    public static void recursivelyCopyDirectory(Path absoluteDirSource, Path absoluteDirDestination) {
        try {
            Files.walkFileTree(absoluteDirSource, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.copy(path, absoluteDirDestination.resolve(absoluteDirSource.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("COPY: " + path + " -> " + absoluteDirDestination.resolve(absoluteDirSource.relativize(path)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.copy(path, absoluteDirDestination.resolve(absoluteDirSource.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("COPY: " + path + " -> " + absoluteDirDestination.resolve(absoluteDirSource.relativize(path)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    System.out.println("ERROR: Copying file '" + path + "' to '" + absoluteDirDestination.resolve(absoluteDirSource.relativize(path)) + "' failed. Exiting...");
                    System.exit(1);
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("ERROR: Copying file '" + absoluteDirSource + "' to '" + absoluteDirDestination + "' failed. Exiting...");
            System.exit(1);
        }
    }

    public static void copy(Path absouluteSourcePath, Path absoluteDestinationPath) {
        if (absouluteSourcePath.toFile().isDirectory()) recursivelyCopyDirectory(absouluteSourcePath, absoluteDestinationPath);
        else {
            try {
                Files.copy(absouluteSourcePath, absoluteDestinationPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioE) {
                System.out.println("ERROR: Copying file '" + absouluteSourcePath + "' to '" + absoluteDestinationPath + "' failed. Exiting...");
                System.exit(1);
            }
        }
    }

}
