#!/bin/bash

# Test video upload
echo "Testing raw video upload..."
curl -X POST http://localhost:8080/api/upload/raw \
  -F "file=@/Users/ghchen/IdeaProjects/videoFlux/sample.jpg" \
  -v

echo -e "\n\nTesting processed video upload..."
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/Users/ghchen/IdeaProjects/videoFlux/sample.jpg" \
  -F "width=200" \
  -F "height=200" \
  -F "quality=80" \
  -F "format=PNG" \
  -v