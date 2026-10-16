# Contributing

How to build, test and release BHOP, and where things live. For what the mod does and how to use
it, see the [README](README.md).

## Pull requests

Bug reports and ideas go through the [issue forms](https://github.com/cire-exe/bunny-hopping/issues/new/choose).
For a larger change, open an issue first so the approach can be agreed before you write it.

1. Fork the repository and create a branch from `main`.
2. Make the change. A change to the movement feel should come with a unit test (see
   [Testing](#testing)).
3. Run `./gradlew build`, which compiles and runs the unit tests.
4. Open a pull request against `main` describing what changed and why.

Leave `CHANGELOG.md` and the version number alone; they are updated when a release is made.
The [Build](.github/workflows/build.yml) workflow runs on every pull request, and a first-time
contributor's run starts once a maintainer approves it.

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

If Gradle cannot find a Java 25 install to run the daemon on, either install one or add the
[Foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) to `settings.gradle` so
Gradle downloads one automatically.

## Testing

### Unit tests

Unit tests live in `src/test` and run as part of every `./gradlew build`, or on their own:

```bash
./gradlew test
```

They cover the movement maths: `SourcePhysics` one function at a time, and `MovementStep` run tick
by tick to check where the speed settles - walking, sprinting, sneaking and using an item on the
ground, stopping, keeping speed across a hop, and air strafing with slowed input. They need no
running game, so they take seconds. A change to the movement feel should come with a test that
pins the new behaviour down.

They are not packaged into the mod jar.

The [Build](.github/workflows/build.yml) workflow runs `./gradlew build`, and so the unit tests, on
every push to `main` and every pull request, and keeps the jar as a download on the run's page.

### Game tests

End-to-end tests live in `src/gametest`. They start a real client in its own folder
(`build/run/clientGameTest`), open a singleplayer world and join a dedicated server with and without
the mod, press keys, run `/bhop` commands and save screenshots of the settings window to
`screenshots/` there. Starting the dedicated server accepts the Minecraft EULA for it. Run them
before a release:

```bash
./gradlew runClientGameTest
```

## Releasing

Releases are published by the [Publish release](.github/workflows/publish.yml) workflow whenever
a `v*` tag is pushed. It builds and tests the mod, creates a GitHub release with the jar attached,
then uploads the jar to [Modrinth](https://modrinth.com/). Both get that version's changelog.

Versions follow [semantic versioning](https://semver.org/): bug fixes bump the patch number
(1.0.1), new features or settings the minor number (1.1.0), and changes that break existing
configs or servers the major number (2.0.0).

1. Run the game tests: `./gradlew runClientGameTest`.
2. Set `version=` in `gradle.properties`.
3. Add a `## <version>` section to the top of `CHANGELOG.md`. Its contents become the release
   notes on GitHub and the changelog on Modrinth.
4. Commit, tag and push:

   ```bash
   git commit -am "Release BHOP 1.0.2"
   git tag -a v1.0.2 -m "BHOP 1.0.2"
   git push origin main v1.0.2
   ```

5. Watch the run in the repository's Actions tab.

The workflow stops with an error, before uploading anything, if the tag does not match `version=`
or `CHANGELOG.md` has no section for it. Fix the cause, then run the workflow by hand from the
Actions tab (**Run workflow**, with the tag name) rather than moving the tag.

If the Modrinth upload fails after the GitHub release was made, such as on an expired token, fix
it and run the workflow by hand the same way: the existing GitHub release is left alone and only
the upload is repeated.

`v*` tags are protected by a repository ruleset: once pushed, they cannot be moved or deleted, so
a published version always points at the code it was built from. A release with a bug in its code
is fixed by releasing the next patch version, not by retagging. `main` is protected the same way
against force pushes and deletion. Both rulesets are under **Settings → Rules → Rulesets**, should
one ever need to be turned off briefly.

Uploading needs the `MODRINTH_TOKEN` repository secret: a Modrinth
[personal access token](https://modrinth.com/settings/pats) with only the **Create versions**
scope. Tokens expire on the date chosen when creating them; once one has, uploads fail until a new
token replaces the secret.

The workflow publishes for the Minecraft version in its `GAME_VERSION` setting. Update it there,
along with `gradle.properties` and `fabric.mod.json`, when moving to a new Minecraft version.

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
│   ├── MovementStep.java      one tick of ground or air movement, from plain inputs
│   ├── BhopMode.java          the player's choice, resolved under the server's rules
│   ├── SourceMovementController.java   per-tick driver: reads the game, jumps and moves
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

src/test/java/com/kishi/bhop/movement/   unit tests for the movement maths
src/gametest/java/com/kishi/bhop/gametest/   end-to-end client and server tests

.github/workflows/build.yml     builds and tests every push and pull request
.github/workflows/publish.yml   publishes tagged releases to GitHub and Modrinth
.github/ISSUE_TEMPLATE/         bug report and feature request forms
.github/dependabot.yml          monthly pull requests for new GitHub Actions versions
CHANGELOG.md                    one section per release, published with it
```

Adding a key binding is one `bind(...)` line in `ModKeyMappings.register()` plus its
`key.bhop.<name>` entry in the lang file.

Adding a physics value means a field in `PhysicsSettings` and one line in its `FIELDS` list. Saving,
repair, the network message and the `/bhop physics` command all follow from that. Bump
`ModPayloads.PROTOCOL` with it, since the network message changes.
