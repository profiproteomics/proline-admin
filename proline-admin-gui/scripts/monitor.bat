java -Xmx4G -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -cp "lib/*;Proline-Admin-GUI-0.8.0-SNAPSHOT.jar;config" -Dlogback.configurationFile=config/logback.xml fr.proline.admin.gui.Monitor %*