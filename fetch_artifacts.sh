#!/bin/sh

: <<'END'
To fetch artifacts from Artifactory using repo and date
$1 repo name  (war-release,war-dev ..etc)
$2 date (use the specific date in run time or sys_date specified in the scipt)
$3 user
$4 password
END

prog=`basename $0`

if [ $# -lt 4 ]; then
    echo "Usage: $prog repo date user password"
    echo "Date should be YYYY-MM-DD"
    exit 1
fi

curl -u $3:$4 -X POST http://<artifactory_server>.com:8081/artifactory/api/search/aql -H "content-type: application/json" -d "items.find({\"type\" : \"file\",\"repo\" :{\"\$match\" : \"$1\"},\"created\":{\"\$lt\":\"${2}T00:00:00.000Z\"}}).include(\"name\",\"repo\",\"path\",\"created\").sort({\"\$asc\": [\"created\"]})"
