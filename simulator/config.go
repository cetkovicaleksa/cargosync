package main

import (
	"encoding/json"
	"flag"
	"log"
	"os"
	"time"
)

type Config struct {
	Mode                string          `json:"mode"`
	WarehouseID         string          `json:"warehouse_id"`
	FactoryID           string          `json:"factory_id"`
	VanID               string          `json:"van_id"`
	RabbitMQURL         string          `json:"rabbitmq_url"`
	Sectors             []SectorConfig  `json:"sectors"`
	Products            []ProductConfig `json:"products"`
	StartLatitude       float64
	StartLongitude      float64
	HeartbeatInterval   time.Duration
	TemperatureInterval time.Duration
	ProductionInterval  time.Duration
}

type SectorConfig struct {
	SectorID        string  `json:"sector_id"`
	SectorName      string  `json:"sector_name"`
	BaseTemperature float64 `json:"base_temperature"`
	Variance        float64 `json:"variance"`
}

type ProductConfig struct {
	ProductID   string `json:"product_id"`
	ProductName string `json:"product_name"`
	MinAmount   int64  `json:"min_amount"`
	MaxAmount   int64  `json:"max_amount"`
}

// rawConfig is used for JSON parsing — durations come in as strings (e.g. "30s", "10m")
type rawConfig struct {
	Mode                string          `json:"mode"`
	WarehouseID         string          `json:"warehouse_id"`
	FactoryID           string          `json:"factory_id"`
	VanID               string          `json:"van_id"`
	RabbitMQURL         string          `json:"rabbitmq_url"`
	Sectors             []SectorConfig  `json:"sectors"`
	Products            []ProductConfig `json:"products"`
	StartLatitude       float64         `json:"start_latitude"`
	StartLongitude      float64         `json:"start_longitude"`
	HeartbeatInterval   string          `json:"heartbeat_interval"`
	TemperatureInterval string          `json:"temperature_interval"`
	ProductionInterval  string          `json:"production_interval"`
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

	if raw.RabbitMQURL == "" {
		log.Fatal("rabbitmq_url is required")
	}

	mode := raw.Mode
	if mode == "" {
		if raw.FactoryID != "" {
			mode = "factory"
		} else {
			mode = "warehouse"
		}
	}

	if mode != "warehouse" && mode != "factory" && mode != "vehicle" {
		log.Fatalf("mode must be one of: warehouse, factory, vehicle (got %q)", mode)
	}

	if mode == "warehouse" {
		if raw.WarehouseID == "" {
			log.Fatal("warehouse_id is required for warehouse mode")
		}
		if len(raw.Sectors) == 0 {
			log.Fatal("at least one sector is required for warehouse mode")
		}
	}

	if mode == "factory" {
		if raw.FactoryID == "" {
			log.Fatal("factory_id is required for factory mode")
		}
		if len(raw.Products) == 0 {
			log.Fatal("at least one product is required for factory mode")
		}
		for i := range raw.Products {
			p := &raw.Products[i]
			if p.ProductID == "" {
				log.Fatalf("products[%d].product_id is required", i)
			}
			if p.ProductName == "" {
				log.Fatalf("products[%d].product_name is required", i)
			}
			if p.MinAmount <= 0 {
				p.MinAmount = 100
			}
			if p.MaxAmount <= 0 {
				p.MaxAmount = 1000
			}
			if p.MaxAmount < p.MinAmount {
				p.MinAmount, p.MaxAmount = p.MaxAmount, p.MinAmount
			}
		}
	}

	if mode == "vehicle" && raw.VanID == "" {
		log.Fatal("van_id is required for vehicle mode")
	}

	heartbeat := parseDurationOrDefault(raw.HeartbeatInterval, 30*time.Second)
	tempInterval := parseDurationOrDefault(raw.TemperatureInterval, 10*time.Minute)
	productionInterval := parseDurationOrDefault(raw.ProductionInterval, 12*time.Hour)

	return &Config{
		Mode:                mode,
		WarehouseID:         raw.WarehouseID,
		FactoryID:           raw.FactoryID,
		VanID:               raw.VanID,
		RabbitMQURL:         raw.RabbitMQURL,
		Sectors:             raw.Sectors,
		Products:            raw.Products,
		StartLatitude:       raw.StartLatitude,
		StartLongitude:      raw.StartLongitude,
		HeartbeatInterval:   heartbeat,
		TemperatureInterval: tempInterval,
		ProductionInterval:  productionInterval,
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
