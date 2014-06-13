#!/bin/bash
# $1 = directory
./timing_tests.sh $1 2 &&
./timing_tests.sh $1 4 &&
./timing_tests.sh $1 8 