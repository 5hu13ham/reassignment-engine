package com.zycus.exception;

public class NoAvailableAgentException extends RuntimeException {

    public NoAvailableAgentException() {
        super("No available agents found for routing");
    }
}
