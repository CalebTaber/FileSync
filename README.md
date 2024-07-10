# FileSync 
### A Java-based utility to synchronize file versions between two directories

### What can I do with FileSync?
- Keep user files up-to-date between your laptop and your desktop
  - Try syncing your Documents folder between your laptop and your desktop. Any changes you make on one system can quickly and easily be synced with the other


- Create backups of your files by syncing with an external hard drive
  - Automate this a step further and set up a cron job to create the backup for you on a schedule


- Copy all your phone photos to your computer
  - Don't manually copy every photo from your phone to your computer. Let FileSync do it for you!


### Building
This project is built and maintained with Maven. After downloading and installing Maven, simply run `mvn package` from the project directory to build the project.

### Running
Driver.java is the entry point of the FileSync program. After building, run a file sync with the following command:  
`java filesync.Driver <directory1-absolute-path> <directory2-absolute-path> <directory1-nickname> <directory2-nickname>`  
Example: `java filesync.Driver /home/user/Documents /mnt/laptop/home/user/Documents desktop laptop`  

Arguments:  
`directory1-absolute-path` is the absolute path to the first directory. There is no enforced ordering of directory1 and directory2, but do make sure the directory nicknames are always associated with the same directory paths  
`directory2-absolute-path` is the absolute path to the second directory  
`directory1-nickname` is the nickname of directory 1  
`directory2-nickname` is the nickname of directory 2  