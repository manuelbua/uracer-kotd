#!/bin/bash
IPADDR=`ip -4 -o addr|grep eth0|awk '{print $4}'|cut -d/ -f1`
echo "Serving on ${IPADDR}:8000... (CTRL+C to stop)"
webfsd -F -r ./build/