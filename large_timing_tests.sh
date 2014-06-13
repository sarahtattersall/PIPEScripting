#!/bin/bash
# $1 = number of threads to run with

dir=$1_threads_results
mkdir -p $dir

FILES=models/pipe5_large_tests/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"


echo "***********" $filename "************"

java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $f -p 100 -t $1 > $dir/$filename.dat 2>&1 &&
java -jar target/uk.ac.imperial-1.0.0-SNAPSHOT-jar-with-dependencies.jar -f $f -s >> $dir/$filename.dat 2>&1
done