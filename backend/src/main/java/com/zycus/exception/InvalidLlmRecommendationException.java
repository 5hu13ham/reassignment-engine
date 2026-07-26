package com.zycus.exception;

public class InvalidLlmRecommendationException extends RuntimeException {

    public InvalidLlmRecommendationException(String message) {
        super(message);
    }
}
