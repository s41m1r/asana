#!/bin/bash

SECONDS=0

while read p; do
  echo "$p"
  echo java -cp extract-asana-events-jar-with-dependencies.jar at.ac.wu.asana.csv.ExtractStructuralDataChanges -csv "$p.csv" -pat "0/7506773dd733d4efc682cd23d5949372" -ws "Springest" -pid "$p"
done < pids.txt

diff=$SECONDS

echo "$(($diff / 3600)) hours, $((($diff / 60) % 60)) minutes and $(($diff % 60)) seconds elapsed."
