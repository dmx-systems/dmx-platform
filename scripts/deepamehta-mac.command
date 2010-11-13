#!/bin/sh
cd "$(dirname "$0")"
java -Dfile.encoding=UTF-8 -Djava.util.logging.config.file=conf/logging.properties -jar bin/felix.jar
