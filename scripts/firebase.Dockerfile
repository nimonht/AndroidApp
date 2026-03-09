# Dockerfile for Firebase CLI tools
# Used by deploy-firebase.sh when Docker mode is selected

FROM node:20-alpine

RUN npm install -g firebase-tools && \
    firebase --version

# Default working directory (project root will be mounted here)
WORKDIR /workspace

ENTRYPOINT ["firebase"]
