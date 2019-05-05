0.4.5:
- Fixed river blocks not being rendered by shaders in recent versions of Forge/OptiFine.
- Fixed river blocks not being rendered by [BSL shaders](https://bitslablab.wixsite.com/main/bsl-shaders) (this works with 7.0 and 7.1.01, but not with earlier versions of 7.1).
- Fixed river blocks ignoring biome-specific water colors when _not_ using OptiFine.

0.4.6:
- Fixed [Streams river blocks disappearing from existing worlds when upgrading to Streams 0.4.5](https://github.com/delvr/Streams/issues/77). (Note: upgrading from 0.4.5 to 0.4.6 will restore the missing blocks but will cause some of the sloping sections to misbehave. For best results, upgrade on the Forge-created world backup instead.)

0.4.7:
- Fixed [a startup crash in Streams 0.4.5 and 0.4.6 when Forge displays any startup error message](https://github.com/delvr/Streams/issues/75).
