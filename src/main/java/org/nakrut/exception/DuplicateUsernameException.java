package org.nakrut.exception;

public class DuplicateUsernameException extends ResourceConflictException {

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username);
    }
}
