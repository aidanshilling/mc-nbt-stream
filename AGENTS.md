# Repository Guidelines

## Project Structure & Module Organization
- Java sources live in `src/main/java/com/nbtstreaming`; `Nbtstreamv2.java` is the Fabric mod entrypoint (`MOD_ID` = `nbt-stream-v2`).
- Mixins and metadata sit in `src/main/resources` (`fabric.mod.json`, `nbt-stream-v2.mixins.json`); client-only mixins live under `src/client`.
- Gradle build logic is in `build.gradle` with versions pinned in `gradle.properties`; target Java 21.
- The `run/` directory holds local dev world data, configs, and server files; avoid committing it.

## Build, Test, and Development Commands
- `./gradlew build` — compile, remap, and package the mod jar.
- `./gradlew runClient` — launch a modded Minecraft client using the `run/` workspace.
- `./gradlew runServer` — start a dedicated server; data is written to `run/`.
- `./gradlew clean` — remove build outputs if caches misbehave.
- `./gradlew check` or `./gradlew test` — run unit tests once they exist.

## Coding Style & Naming Conventions
- Target Java 21; use 4-space indentation and avoid tabs.
- Keep packages lowercase (`com.nbtstreaming`), classes/interfaces in PascalCase, constants in `UPPER_SNAKE_CASE`, and command literals in lowercase.
- Use the Fabric logger (`LoggerFactory.getLogger(MOD_ID)`) and keep command registration inside `onInitialize`.
- Keep mixin classes in `com.nbtstreaming.mixin` and map them via the corresponding `*.mixins.json` files.

## Testing Guidelines
- No automated tests are present yet; add JUnit tests under `src/test/java` named `*Test` with fixtures in `src/test/resources`.
- Run `./gradlew test`; for command behavior, also validate via `runClient`/`runServer` and keep the chat/log snippet that proves the change.

## Commit & Pull Request Guidelines
- Use concise, imperative commit subjects (e.g., `Add radius validation to /pos`); Conventional Commit scopes like `feat: add entity filter` are welcome but optional.
- In PRs, include a summary, linked issue (if any), and screenshots or log snippets when command output changes.
- Note which commands you ran (e.g., `./gradlew build`, `./gradlew runClient`) and mention new dependencies added in `build.gradle`/`gradle.properties`.

## Security & Configuration Tips
- Do not commit personal worlds, caches, or secrets in `run/` or `logs/`; keep the repo focused on source and config.
- When adding dependencies, prefer version bumps via `gradle.properties` for consistency and document any minimum loader/Minecraft version changes in `fabric.mod.json`.
