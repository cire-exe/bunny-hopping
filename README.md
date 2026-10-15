# BHOP

A Fabric mod for **Minecraft 26.3** that adds classic FPS movement: bunny hopping and air
strafing. Install it on your client to play; install it on a server as well to give everyone the
same physics and set rules for the toggle (see [Multiplayer](#multiplayer)).

## Features

### Bunny hop mode

A toggleable movement mode that replaces vanilla physics with the classic FPS movement model. While
it is on you get real air strafing: hold a strafe key, turn the mouse the same way, and you gain
speed on every hop.

The acceleration maths is the classic one, unchanged:

| Behaviour | Function | Implemented in |
| --- | --- | --- |
| Ground friction | `Friction` | `SourcePhysics#applyFriction` |
| Ground acceleration | `Accelerate` | `SourcePhysics#accelerate` |
| Air acceleration | `AirAccelerate` | `SourcePhysics#airAccelerate` |

Two details make bunny hopping work, and both are reproduced faithfully:

1. **The jump is resolved before friction.** Leaving the ground on the tick you land means friction
   never runs, so you keep all of your speed.
2. **The air wish speed is clamped, the acceleration is not.** The clamp limits how much velocity
   can be added *along* the direction you are asking for, but nothing limits the total. Strafing
   keeps your wish direction nearly perpendicular to your velocity, so each tick adds a little speed
   sideways, which rotates and lengthens the velocity vector.

Air strafing follows the mouse **between ticks**. Minecraft simulates movement 20 times a second,
but the view turns every frame, so the mod records where you looked on each frame and replays air
acceleration in 3 sub-steps per tick along that path. That is 60 steps a second, close to the 64
ticks of competitive shooter servers, so strafes are smooth and speed builds at the familiar rate
rather than a third of it.
The number of sub-steps is fixed, not one per frame, so the gain is the same at any frame rate.

Jumping, gravity and speed stay vanilla's own, so a normal one-block jump still clears a block and
the mode blends with ordinary Minecraft terrain. Everything vanilla feeds into them still applies:

- **Effects** - Speed, Slowness, Jump Boost, Slow Falling and Levitation
- **Input** - walking is slower than sprinting, and sneaking, crawling, eating or drawing a bow slow
  you down
- **Blocks** - ice is slippery and honey cuts your jump. Blocks that slow you every tick - soul
  sand, honey and slime underfoot, cobwebs, berry bushes, powder snow - hand movement back to
  vanilla while they hold you, as water and ladders do, so they slow you exactly as much as usual
- **Attributes** - movement speed, jump strength, gravity and friction, from commands or other mods

Hopping is timed by hand by default: each hop needs a jump on the landing tick, and the
mouse wheel is a jump key. Auto hop (hold jump to chain hops) and a jump buffer (a press a few ticks
early still hops on landing) can be turned on for an easier start.

Beneath the speedometer, each hop is scored: the speed it **gained** or lost, whether it was
**perfect** (jumped on the landing tick, with no friction at all) or how many ticks late, and your
strafe **sync** - how much of your mouse turning in the air matched the strafe key held.

Anything with its own specialised movement - water, lava, flight, elytra, ladders, vehicles,
spectator - is left to vanilla, so the mode never breaks normal play.

## Controls

All three bindings appear under a **BHOP** category in *Options -> Controls -> Key Binds* and can
be rebound like any vanilla control.

| Action | Default |
| --- | --- |
| Toggle bunny hop mode | `V` |
| Open settings | `U` |
| Reload config | `R` |

With **Scroll wheel jump** on (the default), turning the mouse wheel jumps while bunny hop mode is
on, instead of changing the hotbar slot.

## Settings window

Press `U` to open the settings. A sidebar on the left switches between **General** (bunny hop
mode, auto hop, jump buffer, scroll wheel jump, speed cap, speed ramp-up) and **HUD** (speedometer, hop gain, hop
timing, strafe sync, toggle message).
Changes apply immediately and are written to `config/bhop.json` when the window closes. The
window does not pause the game or blur the world, so the effect of a change shows right away.

## Configuration

Written to `config/bhop.json` on first launch. Every physics value is exposed, so the feel can be
retuned without recompiling. The `physics` section applies in singleplayer and in worlds you open to
LAN; a server running the mod sends its own instead.

```jsonc
{
  "physics": {
    "groundAccelerate": 5.5,   // sv_accelerate
    "airAccelerate": 12.0,     // sv_airaccelerate - raise for faster strafe gain
    "friction": 5.2,           // sv_friction
    "stopSpeed": 2.0,          // sv_stopspeed, blocks/s
    "maxGroundSpeed": 5.6,     // sprinting ground top speed, blocks/s (~vanilla sprint)
    "maxAirWishSpeed": 0.75,   // the classic 30 u/s air clamp, converted at 40 u/block
    "maxTotalSpeed": 10.43,    // top speed, blocks/s; raise for an uncapped feel
    "airSubsteps": 3,          // air strafe steps per tick: 3 = 60/s, near a 64-tick server; 1 = 20/s
    "rampUp": false,           // boost air gain until rampUpSpeed, for quicker opening hops
    "rampUpSpeed": 8.0,        // the boost stops at this speed, blocks/s
    "rampUpMultiplier": 2.0,   // how much the air wish cap is scaled below rampUpSpeed
    "sprintJumpBoost": 4.0,    // vanilla sprint jump impulse, blocks/s (vanilla is 0.2/tick)
    "sprintJumpSpeed": 7.13,   // ceiling that boost may reach; strafing is needed to go faster
    "jumpScale": 1.0,          // times vanilla's jump, Jump Boost and honey included
    "gravityScale": 1.0        // times vanilla's gravity, Slow Falling included
  },
  "movement": {
    "autoHop": false,          // true to chain hops by holding jump
    "jumpBufferTicks": 0,      // a press this many ticks before landing still hops; 0 = exact
    "scrollJump": true         // the mouse wheel jumps while bhop mode is on
  },
  "hud": {
    "showSpeed": true,         // speed readout above the hotbar while the mode is on
    "speedTextScale": 0.56,    // 1.0 is the normal 9px font
    "speedOffsetY": 60,        // height above the bottom of the screen
    "showGain": true,          // under the speedometer: speed gained or lost on the last hop
    "showHopTiming": true,     // ... perfect hop, or how many ticks late
    "showSync": true,          // ... how well the mouse turning matched the strafe key
    "announceToggle": false,   // brief note above the hotbar on mode toggle
    "statusTextShrinkPixels": 10 // how many screen pixels smaller than vanilla that note is
  }
}
```

The defaults already use manual timing: no auto hop, no jump buffer, and the mouse wheel as
a jump key. For an easier start set `autoHop` to `true` and `jumpBufferTicks` to `2`. For the classic
ground speed, raise `maxGroundSpeed` toward `6.25` (250 u/s).

Physics values that are out of range are clamped, with a warning in the log naming the key: speeds
to at most 100 blocks/s, `groundAccelerate`, `airAccelerate` and `friction` to 1000, and the
multipliers (`rampUpMultiplier`, `jumpScale`, `gravityScale`) to 10. The same limits apply to
physics a server sends, so no server can push values that break the movement maths.

Older config files carrying `jumpVelocity`, `gravity` or `verticalDrag` still load; those keys are
ignored now that the values come from vanilla, and they drop out the next time the file is saved.
Files from before the `physics` section existed kept those values under `movement`; they are moved
across on the first launch, so your tuning is kept.

## Multiplayer

Bunny hop mode needs the mod on both the player's client and the server. A client with the mod can
still join servers without it, with the mode off. With the mod installed, the server decides the
physics and the rules, and players without the mod can still join as normal.

| Server | What happens |
|---|---|
| Runs the mod | Everyone moves with the server's physics. Players toggle the mode within the server's rules. The physics settings are greyed out while connected. |
| Doesn't run the mod | Bunny hop mode is off and can't be turned on. |
| Runs a different version | Treated as a server without the mod, with a message saying so. |
| Singleplayer or LAN | Your own settings apply. Guests of a world you open to LAN receive your physics, and get any changes when you close the settings window. |

The server's half costs effectively nothing. It has no tick handler and no movement hooks, and it
only does work when a player joins, leaves or toggles the mode, or when an admin changes a setting.

### Server config

Written to `config/bhop-server.json` on first start:

```jsonc
{
  "rules": {
    "policy": "OPTIONAL",        // OPTIONAL, FORCED_ON (always on) or DISABLED
    "allowAutoHop": true,        // false makes every hop a timed press
    "maxJumpBufferTicks": 10,    // players' jump buffers are capped at this; 0 = exact timing
    "toggleCooldownTicks": 10,   // ticks between toggles, up to 200
    "requiredPermissionLevel": 0 // 0 = everyone, 2 = operators, up to 4
  },
  "physics": { /* the same keys as the client's physics section */ }
}
```

A world opened to LAN uses the `rules` from here too, but not the `physics`.

### Commands

`/bhop` needs permission level 2. Every change is saved and sent to everyone at once, so nobody
has to rejoin.

| Command | Does |
|---|---|
| `/bhop status` | Shows the rules, and which players have the mod and the mode on |
| `/bhop reload` | Re-reads `bhop-server.json` |
| `/bhop policy <optional\|forced_on\|disabled>` | Sets the policy |
| `/bhop rule <allowAutoHop\|maxJumpBuffer\|toggleCooldown\|permissionLevel> <value>` | Sets a rule |
| `/bhop physics <key> [value]` | Shows or sets a physics value, such as `maxTotalSpeed` |
| `/bhop physics reset` | Restores the default physics |

## Building

Requires **JDK 25** - Minecraft 26.3 and Fabric Loom will not build on anything older, and Loom
needs the Gradle *daemon itself* to be on 25, not just the compiler.

`gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`, so Gradle selects a Java 25 daemon
itself. You do not need to set `JAVA_HOME`, even if the `java` on your PATH is older.

```bash
./gradlew build
```

The jar lands in `build/libs/`. To launch a development client:

```bash
./gradlew runClient
```

End-to-end tests live in `src/gametest`. They start a real client in its own folder
(`build/run/clientGameTest`), open a singleplayer world and join a dedicated server with and without
the mod, press keys, run `/bhop` commands and save screenshots of the settings window to
`screenshots/` there. Starting the dedicated server accepts the Minecraft EULA for it. Run them
before a release:

```bash
./gradlew runClientGameTest
```

If Gradle cannot find a Java 25 install to run the daemon on, either install one or add the
[Foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) to `settings.gradle` so
Gradle downloads one automatically.

## Notes on 26.3

Minecraft 26.1 was the first version to ship **unobfuscated**, and Fabric discontinued Yarn after
1.21.11. This project therefore has no `mappings` dependency, uses the non-remapping
`net.fabricmc.fabric-loom` plugin, and declares mod dependencies with plain `implementation` rather
than `modImplementation`.

## Project layout

```
src/main/java/com/kishi/bhop/        shared with servers
├── BhopMod.java               common entry point: mod id, logger, network and server setup
├── config/                    JSON file plumbing and value repair
├── physics/                   PhysicsSettings: the server-controllable values, defined once
├── rules/                     server rules, and the per-player view of them that gets sent
├── network/                   the three payloads, with the protocol version in their channels
└── server/                    BhopServer (join, toggle checks, broadcasts), config, /bhop

src/client/java/com/kishi/bhop/
├── BhopClient.java            entry point and tick wiring
├── config/                    JSON config
├── session/                   the current server's physics and rules, and the toggle checks
├── input/                     key bindings
├── gui/                       settings window: one SettingsPage per section or sub-page
├── hud/                       speedometer, hop feedback and toggle message
├── movement/
│   ├── SourcePhysics.java     pure movement maths, no Minecraft state
│   ├── BhopMode.java          the player's choice, resolved under the server's rules
│   ├── SourceMovementController.java   per-tick driver
│   ├── VanillaModifiers.java  effects, attributes and blocks that still apply
│   ├── MoveInput.java         movement keys, with vanilla's slowdowns
│   ├── ViewSampler.java       per-frame view path for sub-tick air strafing
│   ├── JumpInput.java         auto hop, jump buffer and scroll wheel jumping
│   └── HopTracker.java        per-hop gain, timing and sync for the HUD
└── mixin/                     the injection points
    ├── LivingEntityTravelMixin.java              bunny hop movement replaces vanilla travel
    ├── LivingEntityJumpMixin.java                suppresses vanilla's duplicate jump
    ├── LivingEntityAccessor.java                 vanilla's jump power and effective gravity
    ├── EntityAccessor.java                       how blocks are slowing the player
    └── MouseHandlerMixin.java                    per-frame view sampling and scroll wheel jump
```

Adding a key binding is one `bind(...)` line in `ModKeyMappings.register()` plus its
`key.bhop.<name>` entry in the lang file.

Adding a physics value means a field in `PhysicsSettings` and one line in its `FIELDS` list. Saving,
repair, the network message and the `/bhop physics` command all follow from that. Bump
`ModPayloads.PROTOCOL` with it, since the network message changes.

## Compatibility

The mod only changes how your own player moves, and only while bunny hop mode is on. Its hooks
add to vanilla code rather than replacing it, and everything it registers is under the `bhop`
namespace, so most mods work alongside it unchanged. A few kinds can overlap:

- **Other movement mods.** While bunny hop mode handles a tick, vanilla's walking and jumping code
  is skipped for your player, so a mod that changes those (double jumps, parkour moves, momentum or
  sprint tweaks) has no effect until the mode is off. Swimming, flight, elytra, ladders and vehicles
  stay vanilla, so mods that change only those are unaffected. Mods that add effects or attributes,
  such as jump strength or gravity, still apply, because the mode reads them from vanilla.
- **Scroll wheel mods.** **Scroll wheel jump** is on by default, so while bunny hop mode is on the
  wheel is taken for jumping in game, and mods that use it there, such as zoom or hotbar scrolling,
  won't receive it. Menus are unaffected. Turn the setting off if you rely on such a mod.
- **Key bindings.** `V`, `U` and `R` are common defaults, so another mod may claim the same key.
  Clashes show in red in *Options -> Controls -> Key Binds*; rebind either one.
- **Anti-cheat.** See [Limitations](#limitations).

## Limitations

- **The rules rely on an honest client.** Movement is simulated on each player's client, as vanilla's
  is, so a modified client could ignore the server's rules. Only a server-side anti-cheat can stop
  that.
- **Anti-cheat plugins** will likely flag bunny hop speeds. Vanilla's own movement checks allow them.
- A change to a player's permission level takes effect when they rejoin or an admin changes a rule.

## Licence

MIT
