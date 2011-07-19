#!/bin/sh
cd "$(dirname "$0")"
java -Dfelix.system.properties=file:conf/config.properties -jar bin/felix.jar
