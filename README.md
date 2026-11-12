# Customizable Java Snake Simulation

The project has a Java simulation engine with a JavaFX adapter. The `.rule` and
`.scenario` files are data interpreted by Java; no second programming language is used.

## Simulation type

This is a **fixed time-step (discrete-time) simulation**. Time moves forward in
equal ticks, and `SimulationEngine.advance()` runs one tick in a fixed order:

1. Apply the queued input and move the snake.
2. Move the enemies.
3. Fire a requested weapon.
4. Resolve collisions.
5. Update effects.
6. Check the objective and levels.
7. Run the player rules.

Every interaction inside a tick is recorded as an `Event`, and the rule engine
reacts to those events. It is not a discrete-event simulation, because no event
queue jumps time to the next event. The scenario's seed makes every run repeatable.

## Entity interactions

| Entities | Interaction |
| --- | --- |
| Snake ↔ food, bonus, weapon | Collected (`Collidable.onCollision`) |
| Snake ↔ enemy, obstacle | Damage or loss (`Collidable.onCollision`) |
| Enemy ↔ obstacle | The obstacle blocks the enemy, which walks around it (`EnemyContact.blocksEnemy`) |
| Enemy ↔ enemy | Two enemies never share a cell (`EnemyContact.blocksEnemy`) |
| Chaser ↔ food | The chaser eats the food, and a new one appears (`EnemyContact.onEnemyEnter`, event `FOOD_STOLEN`) |

| Enemy kind | Default movement | Speed | Eats food |
| --- | --- | --- | --- |
| `CHASER` | `CHASE` (towards the snake) | every 2nd tick | yes |
| `PATROLLER` | `PATROL` (fixed loop) | every tick | no |
| `WANDERER` | `WANDER` (random, seeded) | every tick | no |

## Run with JavaFX

Install or unpack a JavaFX SDK, then run these commands from the repository root.
Replace `JAVAFX_LIB` with the SDK's `lib` directory.

```bash
export JAVAFX_LIB=/path/to/javafx-sdk/lib
rm -rf /tmp/snake-game-classes
mkdir -p /tmp/snake-game-classes
javac --module-path "$JAVAFX_LIB" --add-modules javafx.controls,javafx.graphics \
  -d /tmp/snake-game-classes \
  $(find snake-game-structured/src -name '*.java' | sort)
java --module-path "$JAVAFX_LIB" --add-modules javafx.controls,javafx.graphics \
  -cp /tmp/snake-game-classes app.Main
```

Run the Java command from the repository root so the application can find
`rules/` and `scenarios/`.

## Controls

- `ENTER`: start the simulation
- Arrow keys or `WASD`: move the snake
- `P`: pause/resume
- `F2`: open the Rule Manager (write, edit, enable, disable or delete rules)
- `F3`: choose a scenario
- `SPACE`: fire the most recently collected weapon
- `R`: restart

## Levels and weapons

A scenario's goal is split evenly across its levels. Each new level adds one
enemy and one hazardous obstacle, and the snake moves faster. Finishing the
last level wins the scenario.

| Weapon | Board symbol | Effect |
| --- | --- | --- |
| `BLASTER` | `W` | Removes the enemy nearest to the snake's head |
| `BOMB` | `B` | Removes every enemy within two cells of the head |

A shot that hits nothing gives the charge back.

## Player-defined rules

Press `F2` to manage rules in the game, or create a file in `rules/` yourself,
for example `rules/my_rule.rule`:

```text
RULE LowHealthEscape PRIORITY 5 WHEN event == TICK AND health <= 1 DO spawn(BONUS, SHIELD)
```

The Java `RuleEngine` parses and validates the file. Adding this rule does not
require a change to `SimulationEngine` or `GameController`.

- Lower `PRIORITY` numbers run first.
- A disabled rule is renamed to `name.rule.disabled` and is not loaded.
- A file that fails to load never stops the other rules from loading. The game
  shows how many files failed, and `F2` shows why.
- Condition fields: `event`, `event.detail`, `score`, `health`, `level`, `tick`,
  `snake.length`, `snake.shield`, `weapons`, `enemies`, and any name set with `set_variable`.
- Events: `TICK`, `FOOD_COLLECTED`, `BONUS_COLLECTED`, `WEAPON_COLLECTED`,
  `WEAPON_USED`, `ENEMY_COLLISION`, `OBSTACLE_COLLISION`, `LEVEL_COMPLETED`,
  `EFFECT_EXPIRED`, `FOOD_STOLEN`.
- Actions: `add_score(n)`, `change_health(n)`, `add_obstacle(n)`, `grow_snake()`,
  `spawn(ENEMY|BONUS|WEAPON, type)`, `end_scenario(WIN|LOSS)`,
  `set_variable(name, value)`, `set_strategy(KIND, CHASE|PATROL|WANDER)`.

`set_strategy` changes how every enemy of one kind moves, including enemies
spawned later. For example, `rules/player_angry_patrollers.rule` makes
patrollers start chasing after each finished level.

## Scenario files

Scenario files use simple key/value data, for example:

```text
name=Survival
seed=20261012
food=2
enemies=2
obstacles=0
weapons=2
health=3
objective=SURVIVE
surviveTicks=300
levels=3
walls=4:3,5:3,6:3,14:11,15:11,16:11,10:11,10:12
rules=player_low_health.rule
```

| Key | Required | Meaning |
| --- | --- | --- |
| `name`, `seed`, `food`, `enemies`, `obstacles`, `weapons`, `health` | yes | World setup; the seed makes placement repeatable |
| `objective` | no (`SCORE`) | `SCORE` or `SURVIVE` |
| `targetScore` / `surviveTicks` | for that objective | The goal |
| `levels` | no (`1`) | How many levels the goal is split into |
| `walls` | no | Fixed solid walls as `column:row` on the 20x15 board |
| `rules` | no (all enabled) | Only these rule files are loaded for the scenario |
| `enemyTypes` | no (`CHASER,PATROLLER`) | Enemy kinds, used in turn for each enemy placed |
| `foodPoints` | no (`10`) | Points per food |
| `bonusTicks` | no (`20`) | How long shield and speed bonuses last |
| `bombRadius` | no (`2`) | Bomb range in cells |

Unknown keys, walls outside the board, and walls that block the snake's start
are rejected with a message.
