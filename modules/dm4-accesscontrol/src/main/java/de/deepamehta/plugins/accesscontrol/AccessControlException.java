package de.deepamehta.plugins.accesscontrol;



@SuppressWarnings("serial")
class AccessControlException extends Exception {

    int statusCode;

    AccessControlException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    int getStatusCode() {
        return statusCode;
    }
}
