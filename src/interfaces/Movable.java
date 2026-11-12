package interfaces;

import game.Direction;

/** A capability for entities whose direction can be changed by a strategy. */
public interface Movable {
    void setMovementDirection(Direction direction);
    Direction getMovementDirection();
}
