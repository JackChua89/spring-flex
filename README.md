# Spring Flex Core with Spring 5.3.x

This branch is a part of CCLAS's in-house work to ensure compatibility between `spring-flex-core` and `spring 5.3.x`.

The modules `spring-flex-core` and `spring-flex-parent` are built and deployed into our Nexus repository located at `http://172.22.22.9/nexus/content/repositories/releases_maven`.

To resolve this library into your local CCLAS project, you may need to add the following repository setting into your `.m2/settings.xml`:

```xml
<repository>
    <id>releases_maven</id>
    <name>Datamine Nexus Releases Maven</name>
    <url>http://172.22.22.9/nexus/content/repositories/releases_maven</url>
</repository>
