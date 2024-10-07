package de.kastenklicker.secureserverbackuplibrary;

import de.kastenklicker.secureserverbackuplibrary.upload.UploadClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Backup {

    private final List<String> excludeFiles;
    private final File backupDirectory;
    private final File serverDirectory;
    private final UploadClient uploadClient;
    private List<String> missingFiles = new ArrayList<>();
    private final long maxBackupDirectorySize;

    /**
     * Class for containing all backup logic.
     * @param excludeFiles files which should be excluded from the backup
     * @param backupDirectory the directory of the backups
     * @param serverDirectory the directory which contains all server files
     * @param uploadClient the specific class of the Upload protocol
     * @param maxBackupDirectorySize the maximum size of the backup directory
     */
    public Backup(List<String> excludeFiles, File backupDirectory, File serverDirectory, UploadClient uploadClient, long maxBackupDirectorySize) {
        this.excludeFiles = excludeFiles;
        this.backupDirectory = backupDirectory;
        this.serverDirectory = serverDirectory;
        this.uploadClient = uploadClient;
        this.maxBackupDirectorySize = maxBackupDirectorySize;
    }

    /**
     * Get all files which shouldn't be included, but an error IOException occurred.
     * @return Missing files list
     */
    public List<String> getMissingFiles() {
        return missingFiles;
    }

    /**
     * Creates a backup with all files in the main directory expect the excluded ones.
     * @return Backup file
     * @throws Exception Zip, Upload Exceptions - just everything
     */
    public File backup() throws Exception {

        // Append backup directory to excluded files list
        excludeFiles.add(backupDirectory.getName());

        // Exclude session locks, because those are locked by paper
        excludeFiles.add("world/session.lock");
        excludeFiles.add("world_nether/session.lock");
        excludeFiles.add("world_the_end/session.lock");

        // Get current time for backup file name
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        LocalDateTime localDateTime = LocalDateTime.now();
        String currentTime = dateTimeFormatter.format(localDateTime);

        // Create backup zip file
        File backupFile = new File(backupDirectory, "backup-"+currentTime+".zip");

        // Compress the server files
        Zip zip = new Zip(backupFile, serverDirectory, excludeFiles);
        zip.zip(serverDirectory);
        zip.finish();
        missingFiles = zip.getMissingFiles();

        // Upload file
        uploadClient.upload(backupFile);

        // Delete oldest file if over limit
        while (isOldestFileMarkedToBeDeleted()) {
            File[] fileArray = backupDirectory.listFiles();
            if (fileArray == null)
                throw new FileNotFoundException(backupDirectory + " isn't a directory!");
            ArrayList<File> files = new ArrayList<>(Arrays.asList(fileArray));
            files.sort(Comparator.comparing(File::lastModified));
            File oldestFile = files.getFirst();
            if (!oldestFile.delete()) {
                throw new RuntimeException("Couldn't delete oldest backup: " + oldestFile);
            }
            files.removeLast();
        }

        return backupFile;
    }

    /**
     * Checks if the oldest file of the backup directory should be deleted.
     * @return if the oldest file should be deleted
     */
    private boolean isOldestFileMarkedToBeDeleted() {
        File[] files = backupDirectory.listFiles();

        // Check if directory has more than 1 child
        if (files != null && files.length <= 1)
            return false;

        long currentSize = Arrays.stream(files).mapToLong(File::length).sum();

        return currentSize > maxBackupDirectorySize;
    }

}
