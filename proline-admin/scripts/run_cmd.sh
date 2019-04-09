#!/bin/sh

java -Xmx8G -XX:+UseG1GC -cp "config:lib/*:proline-admin-${pom.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.RunCommand $@
