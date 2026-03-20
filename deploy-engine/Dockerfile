FROM ubuntu:22.04

# Install basic dependencies
RUN apt-get update && apt-get install -y \
    curl git unzip xz-utils libglu1-mesa \
    python3 nodejs npm \
    && rm -rf /var/lib/apt/lists/*

# Install Flutter SDK
RUN git clone https://github.com/flutter/flutter.git -b stable /usr/local/flutter
ENV PATH="/usr/local/flutter/bin:/usr/local/flutter/bin/cache/dart-sdk/bin:${PATH}"

# Prepare Flutter for Web
RUN flutter doctor
RUN flutter config --enable-web

# Setup working directory
WORKDIR /app

# Install Node.js dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the engine code
COPY . .

EXPOSE 8080
CMD ["npm", "start"]
