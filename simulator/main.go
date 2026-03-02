package main

import (
	"fmt"
	"log"
	"math/rand"
	"time"
)

func main() {
	cfg := loadConfig()
	rand.Seed(time.Now().UnixNano())

	if cfg.Mode == "factory" {
		log.Printf("starting simulator for factory %s with %d product(s)", cfg.FactoryID, len(cfg.Products))
		log.Printf("heartbeat every %v, production every %v", cfg.HeartbeatInterval, cfg.ProductionInterval)
	} else {
		log.Printf("starting simulator for warehouse %s with %d sector(s)", cfg.WarehouseID, len(cfg.Sectors))
		log.Printf("heartbeat every %v, temperature every %v", cfg.HeartbeatInterval, cfg.TemperatureInterval)
	}

	pub := newPublisher(cfg.RabbitMQURL)
	defer pub.close()

	done := make(chan struct{})
	if cfg.Mode == "factory" {
		go runFactoryHeartbeat(pub, cfg, done)
		go runFactoryProduction(pub, cfg, done)
	} else {
		go runHeartbeat(pub, cfg, done)
		go runTemperature(pub, cfg, done)
	}

	// Block forever (Ctrl-C to stop).
	<-done
}

// runHeartbeat publishes a heartbeat on every HeartbeatInterval tick.
func runHeartbeat(pub *Publisher, cfg *Config, done chan struct{}) {
	ticker := time.NewTicker(cfg.HeartbeatInterval)
	defer ticker.Stop()

	// Send one immediately so the warehouse goes online right away.
	sendHeartbeat(pub, cfg)

	for {
		select {
		case <-ticker.C:
			sendHeartbeat(pub, cfg)
		case <-done:
			return
		}
	}
}

func sendHeartbeat(pub *Publisher, cfg *Config) {
	msg := HeartbeatMessage{
		WarehouseID: cfg.WarehouseID,
		Type:        "heartbeat",
		Timestamp:   time.Now().UTC().Format(time.RFC3339),
	}
	routingKey := fmt.Sprintf("warehouse.%s.heartbeat", cfg.WarehouseID)
	if err := pub.publish(heartbeatExchange, routingKey, msg); err != nil {
		log.Printf("heartbeat publish error: %v", err)
	} else {
		log.Printf("[heartbeat] warehouse=%s", cfg.WarehouseID)
	}
}

// runTemperature publishes one message with all sector readings every TemperatureInterval.
func runTemperature(pub *Publisher, cfg *Config, done chan struct{}) {
	ticker := time.NewTicker(cfg.TemperatureInterval)
	defer ticker.Stop()

	// current tracks the simulated temperature per sector index.
	current := make([]float64, len(cfg.Sectors))
	for i, s := range cfg.Sectors {
		current[i] = s.BaseTemperature
	}

	sendTemperature(pub, cfg, current)

	for {
		select {
		case <-ticker.C:
			for i, s := range cfg.Sectors {
				current[i] = nextTemperature(current[i], s.BaseTemperature, s.Variance)
			}
			sendTemperature(pub, cfg, current)
		case <-done:
			return
		}
	}
}

func sendTemperature(pub *Publisher, cfg *Config, current []float64) {
	ts := time.Now().UTC().Format(time.RFC3339)
	readings := make([]SectorReading, len(cfg.Sectors))
	for i, s := range cfg.Sectors {
		readings[i] = SectorReading{
			SectorID:    s.SectorID,
			SectorName:  s.SectorName,
			Temperature: round1(current[i]),
		}
		log.Printf("[temperature] warehouse=%s sector=%s temp=%.1f°C", cfg.WarehouseID, s.SectorName, current[i])
	}

	msg := TemperatureMessage{
		WarehouseID: cfg.WarehouseID,
		Type:        "temperature",
		Timestamp:   ts,
		Readings:    readings,
	}
	routingKey := fmt.Sprintf("warehouse.%s.temperature", cfg.WarehouseID)
	if err := pub.publish(dataExchange, routingKey, msg); err != nil {
		log.Printf("temperature publish error: %v", err)
	}
}

func runFactoryHeartbeat(pub *Publisher, cfg *Config, done chan struct{}) {
	ticker := time.NewTicker(cfg.HeartbeatInterval)
	defer ticker.Stop()

	sendFactoryHeartbeat(pub, cfg)

	for {
		select {
		case <-ticker.C:
			sendFactoryHeartbeat(pub, cfg)
		case <-done:
			return
		}
	}
}

func sendFactoryHeartbeat(pub *Publisher, cfg *Config) {
	msg := FactoryHeartbeatMessage{
		FactoryID: cfg.FactoryID,
		Type:      "heartbeat",
		Timestamp: time.Now().UTC().Format(time.RFC3339),
	}
	routingKey := fmt.Sprintf("factory.%s.heartbeat", cfg.FactoryID)
	if err := pub.publish(factoryHeartbeatExchange, routingKey, msg); err != nil {
		log.Printf("factory heartbeat publish error: %v", err)
	} else {
		log.Printf("[heartbeat] factory=%s", cfg.FactoryID)
	}
}

func runFactoryProduction(pub *Publisher, cfg *Config, done chan struct{}) {
	ticker := time.NewTicker(cfg.ProductionInterval)
	defer ticker.Stop()

	sendFactoryProduction(pub, cfg)

	for {
		select {
		case <-ticker.C:
			sendFactoryProduction(pub, cfg)
		case <-done:
			return
		}
	}
}

func sendFactoryProduction(pub *Publisher, cfg *Config) {
	readings := make([]ProductReading, len(cfg.Products))
	for i, p := range cfg.Products {
		amount := p.MinAmount
		if p.MaxAmount > p.MinAmount {
			amount = p.MinAmount + rand.Int63n(p.MaxAmount-p.MinAmount+1)
		}
		readings[i] = ProductReading{
			ProductID:   p.ProductID,
			ProductName: p.ProductName,
			Amount:      amount,
		}
		log.Printf("[production] factory=%s product=%s amount=%d", cfg.FactoryID, p.ProductName, amount)
	}

	msg := FactoryProductionMessage{
		FactoryID: cfg.FactoryID,
		Type:      "production",
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		Readings:  readings,
	}

	routingKey := fmt.Sprintf("factory.%s.production", cfg.FactoryID)
	if err := pub.publish(factoryDataExchange, routingKey, msg); err != nil {
		log.Printf("factory production publish error: %v", err)
	}
}

// nextTemperature generates temperature = base + random_float(-variance, +variance)
// with a small mean-reversion so it doesn't drift too far over many ticks.
func nextTemperature(current, base, variance float64) float64 {
	noise := rand.Float64()*2*variance - variance
	reversion := (base - current) * 0.1
	return current + noise + reversion
}

// round1 rounds a float to 1 decimal place.
func round1(v float64) float64 {
	return float64(int(v*10+0.5)) / 10
}
