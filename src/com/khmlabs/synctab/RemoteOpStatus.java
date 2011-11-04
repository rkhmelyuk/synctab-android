package com.khmlabs.synctab;

/**
 * The status of remote operation.
 */
public enum RemoteOpStatus {

    /** Remote operation succeeded. */
    Success,

    /** Remote operation failed. */
    Failed,

    /** Remote operation didn't happen, so it was queued. */
    Queued
}
