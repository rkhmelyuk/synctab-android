package com.khmlabs.synctab;

public class RegistrationStatus {

    private final String email;
    private final String password;

    private Status status;
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