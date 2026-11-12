package interfaces;

import entity.Snake;

/** A capability for a collectible that applies and expires an effect. */
public interface EffectSource {
    void applyEffect(Snake snake);
    void updateEffect(Snake snake);
    boolean isExpired();
    String effectName();
}
