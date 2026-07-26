package com.example.aics.exception;

public class LibraryFullException extends RuntimeException {

    public LibraryFullException(String message) {
        super(message);
    }
}
