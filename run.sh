#!/bin/bash
cd "$(dirname "$0")"
export MAVEN_OPTS="-XstartOnFirstThread"
mvn clean compile exec:java -Dexec.mainClass="com.dodgingbullets.desktop.Game"
