- Fixed [stone "bridges" appearing across Streams](https://github.com/delvr/Streams/issues/109#issuecomment-717107100).
- Added an option to restore the legacy Streams generation from Streams 0.4.7. Enabling legacy generation reverts a change
  in Streams 0.4.8 that prevented [infinite recursion crash in some modpacks](https://github.com/delvr/Streams/issues/74), 
  but also caused [Streams to become too rare or too small](https://github.com/delvr/Streams/issues/107).
  If you're starting a new world and not using a modpack affected by the crash, you can enable this option by either
  adding `-Dstreams.legacyGeneration=true` to your server's Java command or launcher installation's JVM arguments, 
  or setting environment variable `STREAM_LEGACY_GENERATION=true`. 
  Once enabled for a new world, that world keeps the same value even if the global setting changes later.
