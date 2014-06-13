#!/bin/bash
#$1 = number of threads

echo "***********" $1 "************"
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -s -ss -g &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -t $1 -sub 1 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -t $1 -sub 5 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $1 -p 100 -ss -g -t $1 -sub 10 
