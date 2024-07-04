package fileSynchronizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class Driver {

    /**
     * args[0] = absolute path to directory 1
     * args[1] = absolute path to directory 2
     * args[2] = local hostname
     * args[3] = remote hostname
     */
    public static void main(String[] args) {
        // Do filesync according to logic in bash script
        // Use .gitignore files to avoid libraries and untracked files
        // Ship with standard .sync_exclude list

        int numArgs = args.length;
        boolean localDirExists = numArgs > 1 && directoryExists(args[0]);
        boolean remoteDirExists = numArgs > 2 && directoryExists(args[1]);

        String[] newArgs = Arrays.copyOf(args, 4);
        Scanner newArgsReader = new Scanner(System.in);
        while (numArgs != 4 || !localDirExists || !remoteDirExists) {
            if (numArgs != 4) {
                System.out.println("Too " + ((numArgs < 4) ? "few" : "many") + " arguments. Usage:");
                System.out.println("filesync <directory1-absolute-path> <directory2-absolute-path> <directory1-nickname> <directory2-nickname>");

                System.out.print("Absolute path to directory 1: ");
                newArgsReader.hasNextLine();
                newArgs[0] = newArgsReader.nextLine();

                System.out.print("Absolute path to directory 2: ");
                newArgsReader.hasNextLine();
                newArgs[1] = newArgsReader.nextLine();

                System.out.print("Nickname for directory 1: ");
                newArgsReader.hasNextLine();
                newArgs[2] = newArgsReader.nextLine();

                System.out.print("Nickname for directory 2: ");
                newArgsReader.hasNextLine();
                newArgs[3] = newArgsReader.nextLine();

                localDirExists = directoryExists(newArgs[0]);
                remoteDirExists = directoryExists(newArgs[1]);
                numArgs = 4;
            }

            if (!localDirExists || !remoteDirExists) {
                String nonexistentDirectory = (!localDirExists) ? newArgs[0] : newArgs[1];
                boolean responseNotRecognized;
                String decision;

                System.out.println("Directory '" + nonexistentDirectory + "' does not exist. Create it now? (y/n)");
                do {
                    newArgsReader.hasNextLine();
                    decision = newArgsReader.nextLine();
                    responseNotRecognized = decision.equalsIgnoreCase("y") && decision.equalsIgnoreCase("n");

                    if (responseNotRecognized) System.out.println("Response '" + decision + "' not recognized. Please enter 'y' or 'n'");
                } while (responseNotRecognized);

                if (decision.equalsIgnoreCase("y")) {
                    try {
                        Files.createDirectories(Path.of(nonexistentDirectory));
                        if (nonexistentDirectory.equals(newArgs[0])) localDirExists = directoryExists(nonexistentDirectory);
                        else remoteDirExists = directoryExists(nonexistentDirectory);
                    } catch (IOException ioE) {
                        System.out.println("Directory '" + nonexistentDirectory + "' could not be created. Exiting...");
                        System.exit(1);
                    }
                } else if (decision.equalsIgnoreCase("n")) {
                    System.out.println("Directory '" + nonexistentDirectory + "' not created. Exiting...");
                    System.exit(0);
                }
            }
        }
        newArgsReader.close();

        FileSynchronizer synchronizer = new FileSynchronizer(newArgs[0], newArgs[1], newArgs[2], newArgs[3], true);
        synchronizer.synchronizeFileTrees();
    }

    public static boolean directoryExists(String path) {
        File directory = Path.of(path).toFile();
        return directory.exists() && directory.isDirectory();
    }

}
