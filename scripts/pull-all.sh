#!/bin/sh
cd $(dirname $0)/..

git pull origin master

for f in */.git/..; do (cd "$f";git pull origin master);done

