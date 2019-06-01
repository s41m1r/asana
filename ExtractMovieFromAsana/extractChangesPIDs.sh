#!/bin/bash

while read p; do
  echo "$p"
  java -cp extract-asana-events-jar-with-dependencies.jar at.ac.wu.asana.csv.ExtractStructuralDataChanges -csv "/home/bala/asanaExtraction/extracted/$p.csv" -pat "0/7506773dd733d4efc682cd23d5949372" -ws "Springest" -pid "$p"
done < pids.txt
