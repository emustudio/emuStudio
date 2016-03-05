#!/bin/bash

VAGRANT_STATUS=`vagrant status`

echo $VAGRANT_STATUS | grep running 2>&1 > /dev/null
if [[ $? -ne 0 ]]; then
  echo $VAGRANT_STATUS | grep saved 2>&1 > /dev/null
  if [[ $? -eq 0 ]]; then
    vagrant resume
  else
    vagrant up --provider=virtualbox
  fi
fi

set -e

vagrant provision
vagrant ssh -c "cd /home/vagrant && java -jar emuStudio.jar" -- -X

