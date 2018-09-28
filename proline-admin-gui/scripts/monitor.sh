#!/bin/sh

java -Xmx1500M -cp "config:lib/*:Proline-Admin-GUI-${pom.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.gui.Monitor $@
