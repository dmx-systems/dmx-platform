#!/bin/sh
cd "$(dirname "$0")"
java -Dfile.encoding=UTF-8 -Dfelix.system.properties=file:conf/config.properties -jar bin/felix.jar
