package fileSynchronizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

public class Driver {

    public static final Scanner USER_INPUT = new Scanner(System.in);

    /**
     * args[0] = absolute path to directory 1
     * args[1] = absolute path to directory 2
     * args[2] = local hostname
     * args[3] = remote hostname
     */
    public static void main(String[] args) {
        int numArgs = args.length;
        boolean localDirExists = numArgs > 1 && directoryExists(args[0]);
        boolean remoteDirExists = numArgs > 2 && directoryExists(args[1]);

        String[] newArgs = Arrays.copyOf(args, 4);
        while (numArgs != 4 || !localDirExists || !remoteDirExists) {
            if (numArgs != 4) {
                System.out.println("Too " + ((numArgs < 4) ? "few" : "many") + " arguments. Usage:");
                System.out.println("filesync <directory1-absolute-path> <directory2-absolute-path> <directory1-nickname> <directory2-nickname>");

                System.out.print("Absolute path to directory 1: ");
                newArgs[0] = USER_INPUT.nextLine();

                System.out.print("Absolute path to directory 2: ");
                newArgs[1] = USER_INPUT.nextLine();

                System.out.print("Nickname for directory 1: ");
                newArgs[2] = USER_INPUT.nextLine();

                System.out.print("Nickname for directory 2: ");
                newArgs[3] = USER_INPUT.nextLine();

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
                    decision = USER_INPUT.nextLine();
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

        FileSynchronizer synchronizer = new FileSynchronizer(newArgs[0], newArgs[1], newArgs[2], newArgs[3], true, false);
        synchronizer.synchronizeFileTrees();

        USER_INPUT.close();
    }

    private static boolean directoryExists(String path) {
        File directory = Path.of(path).toFile();
        return directory.exists() && directory.isDirectory();
    }

}
