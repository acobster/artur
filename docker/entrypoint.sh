#!/bin/bash

# Run in Daemon mode
/usr/bin/deluged

# Mount a volume into /app/config to override runtime
java -cp /app/config -jar /app/target/artur.jar
