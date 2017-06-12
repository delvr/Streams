# Streams

The Streams mod introduces real flowing rivers, with a true current, to Minecraft.
These rivers are generated in the world using custom non-decaying flowing blocks and are much larger than anything the
player could create using buckets. They originate in multiple sources and flow down the terrain through slopes and waterfalls,
joining together into wider rivers until they reach a body of water at sea level.

Please note that the source code is in [Scala](http://scala-lang.org) (not Java), and that most of it will be replaced as part of an upcoming major rewrite.
Keeping that in mind, if you have any questions about the code please send me (delvr) a message here on GitHub.
For help with the build process please read [Getting started with ForgeGradle](http://www.minecraftforge.net/forum/index.php/topic,14048.0.html) first.

Questions about the mod itself are best posted to the [discussion thread](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2346379-streams-real-flowing-rivers).

Note: IDE-specific instructions are for IntelliJ IDEA; see the ForgeGradle documentation for Eclipse equivalents.

## Dependencies Setup
Streams compilation requires [Farseek](https://github.com/delvr/Farseek) and [TerraFirmaCraft](https://github.com/Deadrik/TFCraft) (TFC).
Compatible versions are specified using [Maven version range syntax](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402)
in the `modDependencies` and `modOptionalDependencies` properties of `gradle.properties`.
The build process of both Farseek and TFC will output `-deobf` and `-src` jars; for each dependency place both jars in Streams's `libs` subdirectory before running `setupDecompWorkspace`.

## IDE Setup
The IDEA `Update Forge` run configuration will run `setupDecompWorkspace` and `genIntellijRuns`.
After running `Update Forge`, synchronize Gradle in IntelliJ IDEA to set up module configs.

If using IntelliJ 2016 or later, make sure the Gradle plugin setting "Create separate module per source set" is NOT checked.

## Testing
Run the generated `Minecraft Client` or `Minecraft Server` configuration.

Note: TFC is required at compile time but optional at runtime. To test without TFC in IDEA,
switch the TFC dependencies from `Compile` to `Provided` in your module properties after synchronizing Gradle.

## Building
Run the `build` configuration. Jars will be generated in `build/libs`.
