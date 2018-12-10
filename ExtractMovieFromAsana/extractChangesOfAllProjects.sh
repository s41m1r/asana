#!/bin/bash

while read p; do
  echo "$p"
  java -cp extract-asana-events-jar-with-dependencies.jar at.ac.wu.asana.csv.ExtractStructuralDataChanges -csv "/home/saimir/ownCloud/PhD/Collaborations/Waldemar/API/$p.csv" -pat "0/7506773dd733d4efc682cd23d5949372" -ws "Springest" -p "$p"
done < allSmileys.txt
