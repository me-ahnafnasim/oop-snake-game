package client;

import entity.Enemy;
import entity.GameObject;
import entity.Snake;
import game.Direction;
import game.MovementStrategy;
import game.Scenario;
import game.SimulationState;
import interfaces.Collidable;
import interfaces.EnemyContact;
import interfaces.Movable;
import interfaces.Updatable;

/**
 * Client-side demonstration of multityping (subtyping with interfaces).
 *
 * <p>ONE Enemy object is created below. The very same object is then assigned to five
 * differently typed variables, and a different operation is executed through each one.
 * Every variable checks the object for one capability it really has; each caller sees
 * only the contract it needs, while the object itself never changes.
 */
public final class MultitypingDemo {

    public static void main(String[] args) throws Exception {
        SimulationState state = new SimulationState(Scenario.standard());
        Snake snake = new Snake(GameObject.COLUMNS / 2 * GameObject.CELL,
                                GameObject.ROWS / 2 * GameObject.CELL, 3);
        state.setSnake(snake);

        // THE one multityped object. It is a GameObject, Collidable, Movable, Updatable AND
        // EnemyContact at the same time.
        Enemy chaser = new Enemy(4 * GameObject.CELL, 5 * GameObject.CELL,
                Enemy.Kind.CHASER, MovementStrategy.named("CHASE"));
        state.addEntity(chaser);

        System.out.println("ONE object, FIVE types: " + chaser.getClass().getName());
        System.out.println();

        // Type 1 — GameObject: every entity on the board is placeable and drawable as a whole.
        GameObject asBoardObject = chaser;
        System.out.println("1) GameObject    -> initial position: ("
                + (int) (asBoardObject.getX() / GameObject.CELL) + ", "
                + (int) (asBoardObject.getY() / GameObject.CELL) + ")");

        // Type 2 — Movable: only movement-capable objects accept a direction command.
        Movable asMovable = chaser;
        asMovable.setMovementDirection(Direction.UP);
        System.out.println("2) Movable       -> direction now: " + asMovable.getMovementDirection());

        // Type 3 — Updatable: the simulation ticks the object through its update behaviour.
        Updatable asUpdatable = chaser;
        for (int i = 0; i < 4; i++) {
            state.advanceTick();
            asUpdatable.update(state);
        }
        System.out.println("3) Updatable     -> after 4 update(state) calls (it hunts the snake), position: ("
                + (int) (asBoardObject.getX() / GameObject.CELL) + ", "
                + (int) (asBoardObject.getY() / GameObject.CELL) + ")");

        // Type 4 — EnemyContact: the object answers the enemy-blocking question through its contract.
        EnemyContact asEnemyContact = chaser;
        Enemy intruder = new Enemy(0, 0, Enemy.Kind.WANDERER,
                MovementStrategy.named("WANDER"));  // only used as an argument
        System.out.println("4) EnemyContact  -> blocks another enemy from its cell: "
                + asEnemyContact.blocksEnemy(intruder));

        // Type 5 — Collidable: the engine asks the object whether it touches the snake.
        Collidable asCollidable = chaser;
        System.out.println("5) Collidable    -> collides with the snake: "
                + asCollidable.collidesWith(state.getSnake()));

        // All five variables hold one and the same object - proof by identity, not equality.
        System.out.println();
        System.out.println("identityHashCode of each variable (one object, five names):");
        System.out.println("  asBoardObject    : " + System.identityHashCode(asBoardObject));
        System.out.println("  asMovable        : " + System.identityHashCode(asMovable));
        System.out.println("  asUpdatable      : " + System.identityHashCode(asUpdatable));
        System.out.println("  asEnemyContact   : " + System.identityHashCode(asEnemyContact));
        System.out.println("  asCollidable     : " + System.identityHashCode(asCollidable));
        Object o1 = asBoardObject, o2 = asMovable, o3 = asUpdatable,
               o4 = asEnemyContact, o5 = asCollidable;
        System.out.println("all five variables are the same instance: "
                + (o1 == o2 && o2 == o3 && o3 == o4 && o4 == o5));
    }
}
