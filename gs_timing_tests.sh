#!/bin/bash


FILES=models/gs_tests/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"


echo "***********" $filename "************"
./gauss_seidel.sh $f > gs_results/$filename.dat 2>&1 

done