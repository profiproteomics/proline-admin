#!/bin/sh

java -cp "config:lib/*:Proline-Admin-${pom.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.RunCommand $@
