FROM ubuntu:22.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Install basic dependencies
RUN apt-get update && apt-get install -y \
    curl git unzip xz-utils libglu1-mesa \
    python3 ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 18 (LTS) - Required for modern libraries
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

# Install Flutter SDK
RUN git clone https://github.com/flutter/flutter.git -b stable /usr/local/flutter
ENV PATH="/usr/local/flutter/bin:/usr/local/flutter/bin/cache/dart-sdk/bin:${PATH}"

# Prepare Flutter for Web
RUN flutter config --enable-web
RUN flutter doctor

# Setup working directory
WORKDIR /app

# Install Node.js dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the engine code
COPY . .

EXPOSE 8080
CMD ["npm", "start"]
