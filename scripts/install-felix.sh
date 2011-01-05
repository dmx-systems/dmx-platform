#!/bin/sh
cd felix-framework-*
mv bin ..
mv bundle ..
mv conf/config.properties ../conf
cd ..
rm -rf felix-framework-*
