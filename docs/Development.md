# Development

Mekanism is developed for Minecraft 26.3 using NeoForge.

## Prerequisites

* Gradle - Version 9.8.0 is known to work, your mileage with other versions may vary
* JDK - Version 25 is the target as that is what Mojang ships.

## Directory Structure

Mekanism uses a multiloader layout (see [multiloader.md](multiloader.md)). The NeoForge build (`:neoforge`) compiles
`common/src` together with `neoforge/src`.

* /common/src - Shared sources and resources, compiled by each loader project
    - /additions
        - /java/mekanism/additions - Code for Mekanism: Additions
        - /resources - Resources for Mekanism: Additions
    - /api/java/mekanism/api - Our API
    - /datagen/generated
        - /mekanism - Generated resources for Mekanism
        - /mekanismadditions - Generated resources for Mekanism: Additions
        - /mekanismgenerators - Generated resources for Mekanism: Generators
        - /mekanismtools - Generated resources for Mekanism: Tools
    - /generators
        - /java/mekanism/generators - Code for Mekanism: Generators
        - /resources - Resources for Mekanism: Generators
    - /main
        - /java/mekanism - Code for Mekanism
        - /resources - Resources for Mekanism
    - /tools
        - /java/mekanism/tools - Code for Mekanism: Tools
        - /resources - Resources for Mekanism: Tools
* /neoforge/src - NeoForge specific sources and resources
    - /{additions,generators,tools,main}/resources/META-INF - mods.toml, access transformers and enum extensions
    - /api/java/mekanism/api/datagen - Data generator helpers of our API
    - /datagen
        - /additions/java/mekanism/additions - Data generators for Mekanism: Additions
        - /generators/java/mekanism/generators - Data generators for Mekanism: Generators
        - /main/java/mekanism - Data generators for Mekanism
        - /tools/java/mekanism/tools - Data generators for Mekanism: Tools
    - /gameTest
      - /main/java/mekanism - Game Tests for Mekanism
    - /integration - Integrations with other mods
    - /main/java/mekanism - NeoForge implementations of the platform services and of loader specific classes
    - /test/java/mekanism - Unit Tests
* /fabric - The Fabric port (see [fabric-port-status.md](fabric-port-status.md))
* /shim - Fabric implementation of the NeoForge APIs used by the shared code
* /docs - Documentation / style information

## Packaging structure

### Mekanism.jar

* /assets - From /common/src/main/resources/assets and /common/src/datagen/generated/mekanism/assets
* /data - From /common/src/datagen/generated/mekanism/data
* /mekanism - .class files from /common/src/{main,api}/java and /neoforge/src/{main,api}/java
* /META-INF - From /common/src/main/resources/META-INF and /neoforge/src/main/resources/META-INF

### Mekanism-api.jar

This jar is for development purposes

* /mekanism - .class files from /common/src/api/java and /neoforge/src/api/java

### MekanismAdditions.jar

* /assets - From /common/src/additions/resources/assets and /common/src/datagen/generated/mekanismadditions/assets
* /data - From /common/src/datagen/generated/mekanismadditions/data
* /mekanism - .class files from /common/src/additions/java and /neoforge/src/additions/java
* /META-INF - From /neoforge/src/additions/resources/META-INF

### MekanismGenerators.jar

* /assets - From /common/src/generators/resources/assets and /common/src/datagen/generated/mekanismgenerators/assets
* /data - From /common/src/datagen/generated/mekanismgenerators/data
* /mekanism - .class files from /common/src/generators/java
* /META-INF - From /neoforge/src/generators/resources/META-INF

### MekanismTools.jar

* /assets - From /common/src/tools/resources/assets and /common/src/datagen/generated/mekanismtools/assets
* /data - From /common/src/datagen/generated/mekanismtools/data
* /mekanism - .class files from /common/src/tools/java
* /META-INF - From /neoforge/src/tools/resources/META-INF

### Mekanism-all.jar

All in one jar.

* /assets - Assets of all above jars
* /data - Data of all above jars (duplicate tags are last tag wins)
* /mekanism - .class files from all above jars
* /META-INF - META-INF of all above jars

## Building

Mekanism is built in the same way that the vast majority of other mods are. If you are using an IDE such as Eclipse or IntelliJ IDEA, the normal ways of building/launching it work. If you are just trying to build the jars yourself without an IDE and are new to building mods, this can be done by downloading the sources from GitHub and in the root directory running `gradlew build` or `gradlew.bat build`. The built jars will be output into the `build/libs/` folder.
