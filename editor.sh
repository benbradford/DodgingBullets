#!/bin/zsh
cd "$(dirname "$0")"
mvn compile -q
java -XstartOnFirstThread -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.dodgingbullets.editor.LevelEditor
