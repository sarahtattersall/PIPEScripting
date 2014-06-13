#!/bin/bash
# $1 = directory
echo "***** 2 cores *****"
./timing_tests.sh $1 2 &&
echo "***** 4 cores *****" &&
./timing_tests.sh $1 4 && 
echo "***** 8 cores *****" &&
./timing_tests.sh $1 8 