package com.aedstudio.exception;

public class LockedTopicException extends RuntimeException {
    public LockedTopicException(String message) {
        super(message);
    }
}
