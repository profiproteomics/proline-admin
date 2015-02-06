#!/bin/sh

java -cp "config:lib/*:Proline-Admin-${admin.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.RunCommand $@
