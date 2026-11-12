package game;

import entity.GameObject;

/** Immutable event produced during one discrete simulation tick. */
public final class Event {
    public enum Type {
        TICK,
        FOOD_COLLECTED,
        BONUS_COLLECTED,
        WEAPON_COLLECTED,
        ENEMY_COLLISION,
        OBSTACLE_COLLISION,
        LEVEL_COMPLETED,
        EFFECT_EXPIRED,
        WEAPON_USED,
        FOOD_STOLEN
    }

    private final Type type;
    private final GameObject subject;
    private final String detail;

    public Event(Type type) {
        this(type, null, "");
    }

    public Event(Type type, GameObject subject, String detail) {
        this.type = type;
        this.subject = subject;
        this.detail = detail == null ? "" : detail;
    }

    public Type getType() {
        return type;
    }

    public GameObject getSubject() {
        return subject;
    }

    public String getDetail() {
        return detail;
    }
}
