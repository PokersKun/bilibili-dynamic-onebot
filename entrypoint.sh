#!/bin/bash

PUID=${PUID:-0}
PGID=${PGID:-0}

# Ensure tmp and cache dirs exist (Skiko needs tmpdir to unpack native libs)
mkdir -p /app/tmp /app/.cache
export TMPDIR=/app/tmp
export HOME=/app

if [ "$PUID" != "0" ]; then
    # Create group if not exists
    getent group "$PGID" > /dev/null 2>&1 || groupadd -g "$PGID" app
    GRP=$(getent group "$PGID" | cut -d: -f1)

    # Create user if not exists
    getent passwd "$PUID" > /dev/null 2>&1 || useradd -u "$PUID" -g "$PGID" -d /app -s /bin/bash -M app
    USR=$(getent passwd "$PUID" | cut -d: -f1)

    chown -R "$PUID:$PGID" /app/tmp /app/.cache /app/data /app/config
    exec gosu "$USR" java -Dskiko.library.path=/app/skiko-native -Djava.io.tmpdir=/app/tmp -Duser.home=/app -jar /app/bilibili-dynamic-onebot.jar /app/data /app/config
else
    exec java -Dskiko.library.path=/app/skiko-native -Djava.io.tmpdir=/app/tmp -Duser.home=/app -jar /app/bilibili-dynamic-onebot.jar /app/data /app/config
fi
