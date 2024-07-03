package fileSynchronizer;

import java.nio.file.Path;


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

        if (args.length < 4) {
            System.out.println("Too few program arguments. Usage:");
            System.out.println("filesync <local_root_path> <remote_root_path> <local_hostname> <remote_hostname>");
        }
        else {
            FileSynchronizer sync = new FileSynchronizer(args[0], args[1], args[2], args[3], true);
            sync.synchronizeFileTrees();
        }
    }

}
