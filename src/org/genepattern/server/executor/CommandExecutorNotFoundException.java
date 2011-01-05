package org.genepattern.server.executor;

/**
 * Thrown by the mapper when there is no CommandExecutor class for the given job.
 * @author pcarr
 */
public class CommandExecutorNotFoundException extends Exception {
    public CommandExecutorNotFoundException() {
    }
    public CommandExecutorNotFoundException(String message) {
        super(message);
    }
    public CommandExecutorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public CommandExecutorNotFoundException(Throwable cause) {
        super(cause);
    }
}
