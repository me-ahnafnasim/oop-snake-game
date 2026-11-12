package interfaces;

import entity.Snake;
import exception.GameException;
import game.SimulationState;

public interface Collidable {
    boolean collidesWith(Snake snake);

    /** Called by the engine when the snake's head enters this object's cell. */
    void onCollision(SimulationState state) throws GameException;
}
