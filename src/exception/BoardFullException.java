package exception;

public class BoardFullException extends InvalidActionException {
    private static final long serialVersionUID = 1L;

    public BoardFullException(String message) {
        super(message);
    }
}
