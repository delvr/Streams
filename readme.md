# Streams

The Streams mod introduces real flowing rivers, with a true current, to Minecraft 1.7.10.
These rivers are generated in the world using custom non-decaying flowing blocks and are much larger than anything the
player could create using buckets. They originate in multiple sources and flow down the terrain through slopes and waterfalls,
joining together into wider rivers until they reach a body of water at sea level.

Please note that the source code is in [Scala](http://scala-lang.org) (not Java), and that most of it will be replaced as part of an upcoming major rewrite.
Keeping that in mind, if you have any questions about the code please send me (delvr) a message here on GitHub.
For help with the build process please read [Getting started with ForgeGradle](http://www.minecraftforge.net/forum/index.php/topic,14048.0.html) first.

Questions about the mod itself are best posted to the [discussion thread](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2346379-streams-real-flowing-rivers).

Note: IDE-specific instructions are for IntelliJ IDEA; see the ForgeGradle documentation for Eclipse equivalents.

## Dependencies Setup

Streams requires [Farseek](https://github.com/delvr/Farseek) and [TerraFirmaCraft](https://github.com/Deadrik/TFCraft) (TFC).
Compatible versions are specified using [Maven version range syntax](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN402)
in the `farseekDependency` and `tfcDependency` properties of `gradle.properties`.
The build process of both Farseek and TFC will output `-deobf` and `-src` jars; for each dependency place both jars in Streams's `lib` subdirectory before running `setupDecompWorkspace`.

## IDE Setup

The IDEA `Update` run configuration will run `setupDecompWorkspace`.
After running `setupDecompWorkspace`, synchronize Gradle in IntelliJ IDEA to set up module configs.

## Building

Run the `build` configuration. Jars will be generated in `build/libs`.

## Testing

Run configs are not in source control but you can create your own using these properties for the client-side:

* Main class: `net.minecraft.launchwrapper.Launch`
* VM options: `-Djava.library.path=$USER_HOME$/.gradle/caches/minecraft/net/minecraft/minecraft_natives/1.7.10 -Dfml.coreMods.load=farseek.core.FarseekCoreMod,com.bioxx.tfc.TFCASMLoadingPlugin,streams.core.StreamsCoreMod`
* Program arguments: `--tweakClass cpw.mods.fml.common.launcher.FMLTweaker --assetsDir $USER_HOME$/.gradle/caches/minecraft/assets --assetIndex 1.7.10 --accessToken=1234 --userProperties={}`

Note: TFC is required at compile time but optional at runtime. To test without TFC in IDEA,
switch the TFC dependencies from `Compile` to `Provided` in your module properties after synchronizing Gradle.
