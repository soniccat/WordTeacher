#/bin/bash

uri="mongodb://192.168.0.78:27017/?directConnection=true&replicaSet=rs0"

echo $uri
echo $1

mongorestore --uri="$uri" "$1"
# add --drop to remove previous data before restoring
