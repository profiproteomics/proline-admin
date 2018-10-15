#!/bin/sh

java -Xmx4G -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -cp "config:lib/*:Proline-Admin-GUI-${pom.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.gui.Wizard $@
