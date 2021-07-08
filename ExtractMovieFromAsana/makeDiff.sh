#!/bin/bash

SECONDS=0

java -cp extract-asana-events-jar-with-dependencies.jar at.ac.wu.asana.util.MakeDiff "$1" "$2" 

diff=$SECONDS

echo "$(($diff / 3600)) hours, $((($diff / 60) % 60)) minutes and $(($diff % 60)) seconds elapsed."
