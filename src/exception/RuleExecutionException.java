package exception;

public class RuleExecutionException extends GameException {
    private static final long serialVersionUID = 1L;

    public RuleExecutionException(String message) {
        super(message);
    }

    public RuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
