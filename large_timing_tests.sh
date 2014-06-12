#!/bin/bash


FILES=models/pipe5_large_tests/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"


echo "***********" $filename "************"

java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $f -p 100 > results/$filename.dat 2>&1 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $f -s >> results/$filename.dat 2>&1
done