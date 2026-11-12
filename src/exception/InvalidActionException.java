package exception;

public class InvalidActionException extends GameException {
    private static final long serialVersionUID = 1L;

    public InvalidActionException(String message) {
        super(message);
    }
}
