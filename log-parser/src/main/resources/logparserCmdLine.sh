#!/bin/sh

/usr/java/jdk1.8.0_151/bin/java -cp "LogViewer-1.0-SNAPSHOT.jar:lib/*" -Dlogback.configurationFile=logback.xml fr.proline.logviewer.txt.LogApp $@

