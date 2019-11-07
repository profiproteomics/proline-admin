#!/bin/sh

java -Xmx1500M  -cp "config:lib/*:proline-admin-${admin.version}.jar" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.RunCommand setup
