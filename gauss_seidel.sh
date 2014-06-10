#!/bin/bash
echo "***********" $1 "************"
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -s -ss -g &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -sub 1 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -sub 5 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -sub 10 
