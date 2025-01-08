#!/bin/sh
cd "$(dirname "$0")"
exec java -Dfile.encoding=UTF-8 -Dfelix.system.properties=file:conf/config.properties --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED -jar bin/felix.jar
