FROM clojure:openjdk-8-tools-deps-bullseye

RUN apt-get update && \
  apt-get install --yes deluged deluge-console

RUN useradd --create-home --shell /bin/bash deluge

RUN mkdir -p /app/deluge && chown -R deluge:deluge /app
WORKDIR /app

USER deluge

COPY deps.edn /app/deps.edn
COPY build.clj /app/build.clj
COPY resources /app/resources
COPY src /app/src

RUN clojure -T:build uberjar

COPY docker/entrypoint.sh /app/entrypoint.sh

ENTRYPOINT ["/bin/sh", "-c", "/app/entrypoint.sh"]
