java -Xmx8G -XX:+UseG1GC -cp "lib/*;proline-admin-${pom.version}.jar;config" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.RunCommand %*
pause