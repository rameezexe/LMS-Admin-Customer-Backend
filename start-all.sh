#!/bin/bash

echo "Starting all Library Management System Microservices..."

# Function to start a service
start_service() {
    SERVICE_NAME=$1
    echo "Starting $SERVICE_NAME..."
    cd "$SERVICE_NAME"
    nohup mvn spring-boot:run > /dev/null 2>&1 &
    cd ..
}

# 1. Start Eureka Server First
start_service "eureka-server"
echo "Waiting 15 seconds for Eureka to start..."
sleep 15

# 2. Start API Gateway
start_service "api-gateway"
sleep 5

# 3. Start Auth Service
start_service "auth-service"

# 4. Start the rest of the services
start_service "catalog-service"
start_service "member-service"
start_service "borrowing-service"
start_service "payment-service"
start_service "notification-report-service"

echo "All services are starting up in the background!"
echo "It may take another 30-60 seconds for all of them to fully register with Eureka."
echo "You can check Eureka Dashboard at: http://localhost:8761"
