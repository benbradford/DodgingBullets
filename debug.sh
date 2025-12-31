cd "$(dirname "$0")"
mvn compile -q
java -XstartOnFirstThread -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.dodgingbullets.desktop.Game
