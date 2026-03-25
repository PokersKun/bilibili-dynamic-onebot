FROM eclipse-temurin:11-jre-jammy

LABEL maintainer="PokersKun"
LABEL description="BiliBili Dynamic OneBot 11"

ENV JAR_URL=https://github.com/PokersKun/bilibili-dynamic-onebot/releases/download/onebot-v3.2.16/bilibili-dynamic-onebot-3.2.16.jar
ENV PUID=0
ENV PGID=0

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl fontconfig fonts-dejavu-core gosu unzip libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app /app/data /app/config /app/tmp /app/.cache /app/skiko-native \
    && chmod 777 /app/tmp /app/.cache

WORKDIR /app

RUN curl -fSL -o /app/bilibili-dynamic-onebot.jar "$JAR_URL"

# Pre-extract Skiko native library from JAR to avoid runtime unpack issues
# Skiko checks skiko.library.path first; if the .so exists there, it skips unpacking
RUN ARCH=$(uname -m) \
    && if [ "$ARCH" = "x86_64" ]; then \
         SKIKO_LIB="libskiko-linux-x64.so"; \
       elif [ "$ARCH" = "aarch64" ]; then \
         SKIKO_LIB="libskiko-linux-arm64.so"; \
       else \
         echo "Unsupported arch: $ARCH"; exit 1; \
       fi \
    && unzip -j -o /app/bilibili-dynamic-onebot.jar "$SKIKO_LIB" -d /app/skiko-native \
    && chmod 644 /app/skiko-native/*.so \
    && ls -la /app/skiko-native/

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

VOLUME ["/app/data", "/app/config"]

ENTRYPOINT ["/app/entrypoint.sh"]
