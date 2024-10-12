package de.kastenklicker.secureserverbackuplibrary.upload;

/**
 * Thrown if an upload related exception is caught.
 */
public class UploadException extends RuntimeException{
    /**
     * Throw unchecked upload exception.
     * @param cause Any sort of exception thrown by upload clients.
     */
    public UploadException(Throwable cause) {
        super("SecureServerBackupLibrary failed to upload the backup.", cause);
    }
}
