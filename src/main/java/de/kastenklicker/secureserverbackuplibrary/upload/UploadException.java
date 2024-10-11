package de.kastenklicker.secureserverbackuplibrary.upload;

public class UploadException extends RuntimeException{
    public UploadException(Throwable cause) {
        super("SecureServerBackupLibrary failed to upload the backup.", cause);
    }
}
