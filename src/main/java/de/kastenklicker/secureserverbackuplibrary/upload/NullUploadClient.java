package de.kastenklicker.secureserverbackuplibrary.upload;

import java.io.File;

/**
 * Null Object class, if backup is disabled.
 */
public class NullUploadClient extends UploadClient {

    /**
     * Constructor of Null Object class.
     */
    public NullUploadClient() {
        super(null, 0, null, null, null);
    }

    @Override
    protected void internalUpload(File file) {}
}
