package main

import (
	"encoding/json"
	"flag"
	"log"
	"os"
	"time"
)

type Config struct {
	WarehouseID         string         `json:"warehouse_id"`
	RabbitMQURL         string         `json:"rabbitmq_url"`
	Sectors             []SectorConfig `json:"sectors"`
	HeartbeatInterval   time.Duration
	TemperatureInterval time.Duration
}

type SectorConfig struct {
	SectorID        string  `json:"sector_id"`
	SectorName      string  `json:"sector_name"`
	BaseTemperature float64 `json:"base_temperature"`
	Variance        float64 `json:"variance"`
}

// rawConfig is used for JSON parsing — durations come in as strings (e.g. "30s", "10m")
type rawConfig struct {
	WarehouseID         string         `json:"warehouse_id"`
	RabbitMQURL         string         `json:"rabbitmq_url"`
	Sectors             []SectorConfig `json:"sectors"`
	HeartbeatInterval   string         `json:"heartbeat_interval"`
	TemperatureInterval string         `json:"temperature_interval"`
}

func loadConfig() *Config {
	configPath := flag.String("config", "config.json", "path to JSON config file")
	flag.Parse()

	f, err := os.Open(*configPath)
	if err != nil {
		log.Fatalf("cannot open config file %q: %v", *configPath, err)
	}
	defer f.Close()

	var raw rawConfig
	if err := json.NewDecoder(f).Decode(&raw); err != nil {
		log.Fatalf("cannot parse config: %v", err)
	}

	if raw.WarehouseID == "" {
		log.Fatal("warehouse_id is required")
	}
	if raw.RabbitMQURL == "" {
		log.Fatal("rabbitmq_url is required")
	}
	if len(raw.Sectors) == 0 {
		log.Fatal("at least one sector is required")
	}

	heartbeat := parseDurationOrDefault(raw.HeartbeatInterval, 30*time.Second)
	tempInterval := parseDurationOrDefault(raw.TemperatureInterval, 10*time.Minute)

	return &Config{
		WarehouseID:         raw.WarehouseID,
		RabbitMQURL:         raw.RabbitMQURL,
		Sectors:             raw.Sectors,
		HeartbeatInterval:   heartbeat,
		TemperatureInterval: tempInterval,
	}
}

func parseDurationOrDefault(s string, def time.Duration) time.Duration {
	if s == "" {
		return def
	}
	d, err := time.ParseDuration(s)
	if err != nil {
		log.Printf("invalid duration %q, using default %v", s, def)
		return def
	}
	return d
}
