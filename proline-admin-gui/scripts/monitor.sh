#!/bin/sh

java -Xmx1500M -cp "config:lib/*:proline-admin-guis-${pom.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.gui.Monitor $@
