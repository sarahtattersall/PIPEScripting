#!/bin/bash
# $1 = directory
# $2 = number of threads

dir=$2_threads_results
echo "Trying to create " $dir
mkdir -p $dir


FILES=$1/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"

./process.sh $f $2 > $dir/$filename.dat 2>&1
done