#!/bin/bash
# $1 = number of threads

dir=$1_threads_gs_results
mkdir -p $dir


FILES=models/gs_tests/*
for f in $FILES
do
  echo "Processing $f file..."
  filename="${f##*/}"


echo "***********" $filename "************"
./gauss_seidel.sh $f $1 > $dir/$filename.dat 2>&1 

done