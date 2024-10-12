package de.kastenklicker.secureserverbackuplibrary;

import de.kastenklicker.secureserverbackuplibrary.upload.UploadClient;
import de.kastenklicker.secureserverbackuplibrary.upload.UploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class for containing all backup logic.
 */
public class Backup {

    private static final Logger LOGGER = LoggerFactory.getLogger("de.kastenklicker.secureserverlibrary");

    private final List<String> includedFiles;
    private final List<String> excludeFiles;
    private final File backupDirectory;
    private final File serverDirectory;
    private final UploadClient uploadClient;
    private final long maxBackupDirectorySize;

    /**
     * Constructor for Backup class.
     * @param includedFiles files which should be included in backup
     * @param excludeFiles files which should be excluded from the backup
     * @param backupDirectory the directory of the backups
     * @param serverDirectory the directory which contains all server files
     * @param uploadClient the specific class of the Upload protocol
     * @param maxBackupDirectorySize the maximum size of the backup directory
     */
    public Backup(List<String> includedFiles, List<String> excludeFiles, File backupDirectory, File serverDirectory, UploadClient uploadClient, long maxBackupDirectorySize) {
        this.includedFiles = includedFiles;
        this.excludeFiles = excludeFiles;
        this.backupDirectory = backupDirectory;
        this.serverDirectory = serverDirectory;
        this.uploadClient = uploadClient;
        this.maxBackupDirectorySize = maxBackupDirectorySize;
    }
    
    /**
     * Creates a backup with all files in the main directory expect the excluded ones.
     * @return Backup file
     */
    public File backup() {

        // Append backup directory to excluded files list
        excludeFiles.add(backupDirectory.getName());

        // Get current time for backup file name
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        LocalDateTime localDateTime = LocalDateTime.now();
        String currentTime = dateTimeFormatter.format(localDateTime);
        
        // Create backup zip file
        File backupFile = new File(backupDirectory, "backup-"+currentTime+".zip");
        LOGGER.debug("Zipping files into {}.", backupFile.getName());

        // Compress the server files
        Zip zip = new Zip(backupFile, serverDirectory, includedFiles, excludeFiles);
        for (File file : zip.getIncludedFiles())
            zip.zip(file);
        zip.finish();
        
        LOGGER.debug("Finished zipping file.");
        
        try {
            // Upload file
            uploadClient.upload(backupFile);
        } catch (Exception e) {
            throw new UploadException(e);
        }

        // Delete oldest file if over limit
        while (isOldestFileMarkedToBeDeleted()) {
            File[] fileArray = backupDirectory.listFiles();
            if (fileArray == null)
                throw new RuntimeException(backupDirectory + " isn't a directory!");
            ArrayList<File> files = new ArrayList<>(Arrays.asList(fileArray));
            files.sort(Comparator.comparing(File::lastModified));
            File oldestFile = files.getFirst();
            if (!oldestFile.delete()) {
                throw new RuntimeException("Couldn't delete oldest backup: " + oldestFile);
            }
            files.removeLast();
            LOGGER.debug("Removed oldest backup file {}.", oldestFile.getName());
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
