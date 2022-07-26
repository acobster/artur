FROM clojure:openjdk-8-tools-deps-bullseye

RUN apt-get update && \
  apt-get install --yes deluged deluge-console

RUN useradd --create-home --shell /bin/bash deluge

RUN mkdir -p /app/deluge
WORKDIR /app

COPY deps.edn /app/deps.edn
COPY build.clj /app/build.clj
COPY build /app/build
COPY src /app/src

COPY docker/entrypoint.sh /app/entrypoint.sh

RUN chown -R deluge:deluge /app && clojure -T:build uberjar

USER deluge

ENTRYPOINT ["/bin/sh", "-c", "/app/entrypoint.sh"]
