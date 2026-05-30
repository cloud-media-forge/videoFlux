#!/bin/bash


if ! command -v java &> /dev/null
then
    echo "Java is not installed. Please install Java 17 or higher."
    exit 1
fi

if command -v mvn &> /dev/null
then
    echo "Building with Maven..."
    mvn clean install
    
    echo "Starting Video Upload API service..."
    mvn spring-boot:run -pl videoFlux-upload-api &
    

    echo "Starting Video Admin UI service..."
    mvn spring-boot:run -pl videoFlux-admin-ui &
    
    echo "All services started. Press Ctrl+C to stop."
    wait
else
    echo "Maven is not installed."
    echo "To run this application, please:"
    echo "1. Install Maven (https://maven.apache.org/install.html)"
    echo "2. Run 'mvn clean install' in the project directory"
    echo "3. Run each service separately:"
    echo "   mvn spring-boot:run -pl videoFlux-upload-api"
    echo "   mvn spring-boot:run -pl videoFlux-admin-ui"
fi