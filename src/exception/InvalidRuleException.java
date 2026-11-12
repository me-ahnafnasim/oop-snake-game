package exception;

public class InvalidRuleException extends GameException {
    private static final long serialVersionUID = 1L;

    public InvalidRuleException(String message) {
        super(message);
    }
}
