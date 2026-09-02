package org.nakrut.exception;

public class InvalidSortFieldException extends RuntimeException{
    public InvalidSortFieldException(String message) {
        super("Unsupported task sort field: " + message);
    }
}
