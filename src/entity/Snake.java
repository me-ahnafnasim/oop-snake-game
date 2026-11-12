package entity;

import game.Direction;
import interfaces.Movable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Snake extends GameObject implements Movable {
    private static final int INITIAL_LENGTH = 4;

    private final List<Segment> body = new ArrayList<>();
    private final Deque<Weapon.Type> weapons = new ArrayDeque<>();
    private Direction direction = Direction.RIGHT;
    private Direction pendingDirection = Direction.RIGHT;
    private boolean growPending = false;
    private int health = 3;
    private int shieldTicks;
    private int speedBoostTicks;

    public Snake(double x, double y) {
        super(x, y);
        for (int i = 0; i < INITIAL_LENGTH; i++) {
            Color c = (i == 0) ? Color.DARKGREEN : Color.LIMEGREEN;
            body.add(new Segment(x - i * CELL, y, c));
        }
    }

    /** Queues a turn for the next tick; a 180-degree reversal is ignored. */
    @Override
    public void setMovementDirection(Direction d) {
        if (d != direction.opposite()) {
            pendingDirection = d;
        }
    }

    @Override
    public Direction getMovementDirection() {
        return direction;
    }

    public void move() {
        direction = pendingDirection;
        double newX = getX() + direction.getDx() * CELL;
        double newY = getY() + direction.getDy() * CELL;

        setPosition(newX, newY);

        if (growPending) {
            growPending = false;
            if (!body.isEmpty()) {
                body.get(0).setColor(Color.LIMEGREEN);
            }
            body.add(0, new Segment(newX, newY, Color.DARKGREEN));
        } else {
            body.remove(body.size() - 1);
            body.add(0, new Segment(newX, newY, Color.DARKGREEN));
            if (body.size() > 1) {
                body.get(1).setColor(Color.LIMEGREEN);
            }
        }
    }

    public void grow() {
        growPending = true;
    }

    public boolean collidesWithSelf() {
        for (int i = 1; i < body.size(); i++) {
            if (body.get(i).getX() == getX() && body.get(i).getY() == getY()) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(double x, double y) {
        for (Segment s : body) {
            if (s.getX() == x && s.getY() == y) {
                return true;
            }
        }
        return false;
    }

    public double getMoveInterval(int level) {
        double[] intervals = {0.26, 0.24, 0.22, 0.20, 0.18, 0.16, 0.14, 0.12, 0.10};
        int idx = Math.max(0, Math.min(level - 1, intervals.length - 1));
        double interval = intervals[idx];
        if (speedBoostTicks > 0) interval = Math.max(0.06, interval - 0.04);
        return interval;
    }

    public int getLength() {
        return body.size();
    }

    public List<Segment> getBody() {
        return List.copyOf(body);
    }

    public int getHealth() { return health; }

    public void setHealth(int health) {
        this.health = Math.max(0, health);
    }

    public void changeHealth(int amount) {
        health = Math.max(0, health + amount);
    }

    public void addShieldTicks(int ticks) {
        shieldTicks = Math.max(shieldTicks, ticks);
    }

    public int getShieldTicks() { return shieldTicks; }

    public boolean consumeShield() {
        if (shieldTicks <= 0) return false;
        shieldTicks--;
        return true;
    }

    public void addSpeedBoostTicks(int ticks) {
        speedBoostTicks = Math.max(speedBoostTicks, ticks);
    }

    public void setSpeedBoostTicks(int ticks) {
        speedBoostTicks = Math.max(0, ticks);
    }

    public int getSpeedBoostTicks() { return speedBoostTicks; }

    public void addWeapon(Weapon.Type type) {
        weapons.addLast(type);
    }

    /** Removes and returns the most recently collected weapon, or null when the snake has none. */
    public Weapon.Type takeWeapon() {
        return weapons.pollLast();
    }

    public void returnWeapon(Weapon.Type type) {
        weapons.addLast(type);
    }

    public List<Weapon.Type> getWeapons() {
        return List.copyOf(weapons);
    }

    public void advanceEffects() {
        if (shieldTicks > 0) shieldTicks--;
        if (speedBoostTicks > 0) speedBoostTicks--;
    }

    @Override
    public void render(GraphicsContext gc) {
        for (Segment s : body) {
            s.render(gc);
        }
        drawEyes(gc);
    }

    private void drawEyes(GraphicsContext gc) {
        double cx = getX() + CELL / 2;
        double cy = getY() + CELL / 2;
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 8, cy - 7, 8, 8);
        gc.fillOval(cx + 1, cy - 7, 8, 8);
        gc.setFill(Color.BLACK);
        gc.fillOval(cx - 6, cy - 5, 4, 4);
        gc.fillOval(cx + 3, cy - 5, 4, 4);
    }
}
