// Package main is the entry point for the routing optimization service.
package main

import (
	"log"
	"os"

	"github.com/akaitigo/mottainai-flow/routing/server"
)

func main() {
	port := os.Getenv("ROUTING_GRPC_PORT")
	if port == "" {
		port = "9091"
	}

	log.Printf("Starting routing service on port %s", port)

	if err := server.Run(port); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
