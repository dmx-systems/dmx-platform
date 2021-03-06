package systems.dmx.files;

import javax.ws.rs.core.Response.Status;



public class FileRepositoryException extends Exception {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Status status;

    // ---------------------------------------------------------------------------------------------------- Constructors

    FileRepositoryException(String message, Status status) {
        super(message);
        this.status = status;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Status getStatus() {
        return status;
    }

    public int getStatusCode() {
        return status.getStatusCode();
    }
}
