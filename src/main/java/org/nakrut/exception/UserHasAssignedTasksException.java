package org.nakrut.exception;

public class UserHasAssignedTasksException extends ResourceConflictException {

    public UserHasAssignedTasksException(Long userId) {
        super("Cannot delete user with assigned tasks: " + userId);
    }
}
