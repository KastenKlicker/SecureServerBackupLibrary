package de.kastenklicker.secureserverbackuplibrary.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Abstract class for uploading to a server.
 */
public abstract class UploadClient {

    /**
     * SLF4J Logger
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger("de.kastenklicker.secureserverlibrary");
    
    /**
     * Hostname of remote server.
     */
    protected final String hostname;
    /**
     * Port of remote server.
     */
    protected final int port;
    /**
     * Username of remote user.
     */
    protected String username;
    /**
     * Password for FTPS, password or path to private RSA key file for SFTP.
     */
    protected final String authentication;
    /**
     * Directory for the backups of the remote server.
     */
    protected final String remoteDirectory;

    /**
     * Constructor of UploadClient class.
     * @param hostname Hostname/Domain/IP Address of remote server.
     * @param port Port of remote server.
     * @param username Username of remote user.
     * @param authentication Authentication either password or file path to unlocked private RSA key.
     * @param remoteDirectory The directory of the remote server.
     */
    public UploadClient(String hostname, int port, String username, String authentication, String remoteDirectory) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.authentication = authentication;
        this.remoteDirectory = remoteDirectory;
    }

    /**
     * Internal method for uploading.
     * @param file File to upload.
     * @throws Exception Any sort of upload exception.
     */
    protected abstract void internalUpload(File file) throws Exception;

    /**
     * Abstract method for uploading.
     * @param file File to upload.
     * @throws UploadException Any sort of upload exception.
     */
    public void upload(File file) throws UploadException {
        LOGGER.info("Start upload to {}", hostname);
        try {
            internalUpload(file);
        } catch (Exception e) {
            throw new UploadException(e);
        }
        LOGGER.info("Finished uploading to {}", hostname);
    }
}
