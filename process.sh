#!/bin/bash
# $1 = file
# $2 = number of threads
echo "***********" $1 "************"
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -s &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -t $2 -p 100 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -t $2 -p 200 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -t $2 -p 500 
# java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 1000
