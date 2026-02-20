#!/bin/bash
set -e

echo "Initializing LocalStack resources..."

# Create S3 bucket
awslocal s3 mb s3://waiveliability-local

# Set CORS on bucket (for direct browser uploads if needed later)
awslocal s3api put-bucket-cors \
  --bucket waiveliability-local \
  --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["http://localhost:4200"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3600
    }]
  }'

echo "LocalStack initialization complete."
