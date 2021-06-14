#!/bin/bash
kill -9 $(ps -ef | grep "java -jar OpenFlow.jar"| grep -v grep | awk '{print $2}')