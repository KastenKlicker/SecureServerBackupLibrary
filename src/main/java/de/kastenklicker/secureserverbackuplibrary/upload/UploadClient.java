package de.kastenklicker.secureserverbackuplibrary.upload;

import java.io.File;

/**
 * Abstract class for uploading to a server.
 */
public abstract class UploadClient {

    protected final String hostname;
    protected final int port;
    protected String username;
    protected final String authentication; // password or path of private key file
    protected final String remoteDirectory;

    public UploadClient(String hostname, int port, String username, String authentication, String remoteDirectory) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.authentication = authentication;
        this.remoteDirectory = remoteDirectory;
    }

    /**
     * Abstract method for uploading.
     * @param file File to upload.
     */
    public abstract void upload(File file) throws Exception;
}
