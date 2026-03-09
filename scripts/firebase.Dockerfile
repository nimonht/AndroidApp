# Dockerfile for Firebase CLI tools
# Used by deploy-firebase.sh when Docker mode is selected

FROM node:20-trixie-slim

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-21-jre-headless && \
    rm -rf /var/lib/apt/lists/* && \
    npm install -g firebase-tools@15.8.0 && \
    java -version && \
    firebase --version

# Default working directory (project root will be mounted here)
WORKDIR /workspace

ENTRYPOINT ["firebase"]
