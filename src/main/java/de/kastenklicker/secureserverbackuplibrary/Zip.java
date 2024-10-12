package de.kastenklicker.secureserverbackuplibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class for zipping the server files.
 */
public class Zip {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("de.kastenklicker.secureserverlibrary");

    private final ZipOutputStream zipOutputStream;
    private final FileOutputStream fileOutputStream;
    private final List<String> excludeFiles;
    private final File serverDirectory;

    /**
     * The constructor for the backup zip file.
     * @param backupFile The zip file.
     * @param serverDirectory The directory where the server.jar is located.
     * @param excludeFiles List of files which should be excluded.
     * @throws ZipException Thrown if file is a directory or can't be accessed/created.
     */
    public Zip(File backupFile, File serverDirectory , List<String> excludeFiles) {
        this.serverDirectory = serverDirectory;

        // Get unique path for every excluded file
        this.excludeFiles = excludeFiles.stream().map(path -> {
                    try {
                        return new File(serverDirectory, path).getCanonicalPath();
                    } catch (IOException e) {
                        LOGGER.debug("IOException occurred for excluded: {}", path);
                        return null;
                    }
                }
        ).filter(Objects::nonNull).toList();

        // Create Zip OutputStream
        try {
            fileOutputStream = new FileOutputStream(backupFile);

            zipOutputStream = new ZipOutputStream(fileOutputStream);
            zipOutputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    /**
     * Zip files and directories recursive.
     * @param file file to add
     * @throws ZipException Wrapped IOException
     */
    public void zip(File file) throws ZipException {

        try {
            // Check if file should be excluded from the backup
            for (String path : excludeFiles) {
                if (file.getCanonicalPath().contains(path))
                    return;
            }

            // If file is a directory, add all of its child files, if there are any
            if (file.isDirectory()) {
                Optional<File[]> optionalFiles = Optional.ofNullable(file.listFiles());
                if (optionalFiles.isPresent()) {
                    for (File child : optionalFiles.get()) {
                        zip(child);
                    }
                }
            } else {

                // File is a real file, so add it
                // Add Entry
                ZipEntry zipEntry = new ZipEntry(file.getAbsolutePath()
                        .replace(serverDirectory.getAbsolutePath(), "").substring(1));

                zipOutputStream.putNextEntry(zipEntry);

                // Write file to zip
                byte[] buf = new byte['?'];

                FileInputStream fileInputStream = new FileInputStream(file);
                int len;
                while ((len = fileInputStream.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }

                zipOutputStream.closeEntry();
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    /**
     * Finish up the zip file
     * @throws ZipException OutputStream Exceptions.
     */
    public void finish() {
        try {
            zipOutputStream.flush();
            zipOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }
}
