#!/bin/bash


FILES=models/gs_tests/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"

./process.sh $f > results/$filename.dat 2>&1
done