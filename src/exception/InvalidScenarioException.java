package exception;

public class InvalidScenarioException extends GameException {
    private static final long serialVersionUID = 1L;

    public InvalidScenarioException(String message) {
        super(message);
    }
}
