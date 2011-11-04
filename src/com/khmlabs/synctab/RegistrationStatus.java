package com.khmlabs.synctab;

/**
 * The status of the registration.
 */
public class RegistrationStatus {

    /** Used email. */
    private final String email;

    /** Used password. */
    private final String password;

    /** The result status of registration. */
    private Status status;

    /** The description message, if failed to register. */
    private String message;

    public RegistrationStatus(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static enum Status {

        /** Registered successfully. */
        Succeed,

        /** Failed to register, usually comes with a message. */
        Failed,

        /** Can't register, as no connection */
        Offline
    }
}