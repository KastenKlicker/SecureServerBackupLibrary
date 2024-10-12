package de.kastenklicker.secureserverbackuplibrary;

import de.kastenklicker.secureserverbackuplibrary.upload.SFTPClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BackupTest {

    File backupsDirectory = new File("./src/test/resources/zipTest/backups");

    /**
     * Setup FileConfiguration and backup directory
     */
    @BeforeEach
    public void setup() {
        if (!backupsDirectory.mkdirs()) {
            throw new RuntimeException("Couldn't create test backup dir. Does it already exists?");
        }
    }

    @Test
    public void testBackup() {

        assertTrue(new Backup(
                List.of("."),
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                new ArrayList<>(),
                1)
                .backup().exists());
    }

    @Test
    public void testBackupSFTP() throws Exception {

        // Generate Test RSA Keys
        KeyPairGenerator keyPairGenerator = new KeyPairGenerator();
        keyPairGenerator.generate();
        File publicHostKey = keyPairGenerator.getPublicKeyFile();
        File privateHostKey = keyPairGenerator.getPrivateKeyFile();
        
        // Create & start Docker Container        
        GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:alpine")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(privateHostKey.getAbsolutePath()),
                        "/etc/ssh/ssh_host_rsa_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload");

        sftpContainer.start();
        
        // The real testing
        SFTPClient uploadClient = new SFTPClient(
                sftpContainer.getHost(),
                sftpContainer.getMappedPort(22), 
                "foo",
                "pass",
                publicHostKey, 
                20000,
                "/upload");
        
        File backup = new Backup(
                List.of("."),
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                List.of(uploadClient),
                1)
                .backup();
        
        assertTrue(backup.exists());
        
        // Clean up
        publicHostKey.delete();
        privateHostKey.delete();

        // Check if file was transferred correctly
        File backupUpload = new File("./src/test/resources/" + backup.getName());
        sftpContainer.copyFileFromContainer("/home/foo/upload/" + backup.getName(), backupUpload.getPath());
        assertEquals(-1L, Files.mismatch(backup.toPath(), backupUpload.toPath()));
        assertTrue(backupUpload.delete());
    }

    @Test
    public void testBackupMaxDirSizeReached() throws Exception {

        // Create RandomAccessFile in backupDir
        RandomAccessFile randomAccessFile = new RandomAccessFile(
                "./src/test/resources/zipTest/backups/temporaryTestFile","rw");

        //randomAccessFile.setLength(1024*1024*1024); // KB -> MB -> GB
        randomAccessFile.setLength(1024*1024*1024); // KB -> MB -> GB

        // Write RandomAccessFile into backups directory
        randomAccessFile.close();

        // Check if it was created
        assertTrue(new File("./src/test/resources/zipTest/backups/temporaryTestFile").exists());

        assertTrue(new Backup(
                List.of("."),
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                new ArrayList<>(),
                1)
                .backup().exists());

        // Check if RandomAccessFile still exist - it shouldn't
        assertFalse(new File("./src/test/resources/zipTest/backups/temporaryTestFile").exists());
    }

    @AfterEach
    public void cleanUp() throws FileNotFoundException {
        
        if (backupsDirectory.listFiles() == null)
            throw new FileNotFoundException(backupsDirectory + " is not a directory.");
        
        for (File file : backupsDirectory.listFiles()) {
            file.delete();
        }
        backupsDirectory.delete();
    }
}
