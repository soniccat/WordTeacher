#/bin/bash

date=$(date '+%Y-%m-%d %H:%M:%S')
uri="mongodb://192.168.0.78:27017/?directConnection=true&replicaSet=rs0"
out="/Users/Shared/WordTeacher/backups/mongo/$date"

echo $date
echo $uri
echo $out

mongodump --uri="$uri" --out="$out" --db=admin
mongodump --uri="$uri" --out="$out" --db=cardSets
mongodump --uri="$uri" --out="$out" --db=cardSetSearch
mongodump --uri="$uri" --out="$out" --db=users
mongodump --uri="$uri" --out="$out" --db=config
