package main

import (
	"encoding/json"
	"fmt"
	"log"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

const (
	heartbeatExchange        = "warehouse.heartbeat"
	dataExchange             = "warehouse.data"
	factoryHeartbeatExchange = "factory.heartbeat"
	factoryDataExchange      = "factory.data"
	vanExchange              = "van.exchange"
	vanHeartbeatKey          = "van.hartbeat"
	vanDataKey               = "van.data"
)

// HeartbeatMessage matches the spec format (snake_case JSON)
type HeartbeatMessage struct {
	WarehouseID string `json:"warehouse_id"`
	Type        string `json:"type"`
	Timestamp   string `json:"timestamp"`
}

// SectorReading is one entry in a temperature message's readings array
type SectorReading struct {
	SectorID    string  `json:"sector_id"`
	SectorName  string  `json:"sector_name"`
	Temperature float64 `json:"temperature"`
}

// TemperatureMessage contains readings for ALL sectors in one publish
type TemperatureMessage struct {
	WarehouseID string          `json:"warehouse_id"`
	Type        string          `json:"type"`
	Timestamp   string          `json:"timestamp"`
	Readings    []SectorReading `json:"readings"`
}

type FactoryHeartbeatMessage struct {
	FactoryID string `json:"factory_id"`
	Type      string `json:"type"`
	Timestamp string `json:"timestamp"`
}

type ProductReading struct {
	ProductID   string `json:"product_id"`
	ProductName string `json:"product_name"`
	Amount      int64  `json:"amount"`
}

type FactoryProductionMessage struct {
	FactoryID string           `json:"factory_id"`
	Type      string           `json:"type"`
	Timestamp string           `json:"timestamp"`
	Readings  []ProductReading `json:"readings"`
}

type VanHeartbeatMessage struct {
	VanID     string `json:"van_id"`
	Type      string `json:"type"`
	Timestamp string `json:"timestamp"`
}

type VanLocation struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type VanDistanceMessage struct {
	VanID     string      `json:"van_id"`
	Type      string      `json:"type"`
	Timestamp string      `json:"timestamp"`
	Location  VanLocation `json:"location"`
	Distance  float64     `json:"distance"`
}

// Publisher wraps an AMQP connection and channel.
// A mutex protects the channel so multiple goroutines can publish safely.
type Publisher struct {
	mu      sync.Mutex
	conn    *amqp.Connection
	channel *amqp.Channel
	url     string
}

func newPublisher(url string) *Publisher {
	p := &Publisher{url: url}
	if err := p.connect(); err != nil {
		log.Fatalf("cannot connect to RabbitMQ: %v", err)
	}
	return p
}

func (p *Publisher) connect() error {
	conn, err := amqp.Dial(p.url)
	if err != nil {
		return fmt.Errorf("dial: %w", err)
	}

	ch, err := conn.Channel()
	if err != nil {
		conn.Close()
		return fmt.Errorf("open channel: %w", err)
	}

	// Declare both exchanges — idempotent, must match Spring Boot config
	for _, ex := range []string{heartbeatExchange, dataExchange, factoryHeartbeatExchange, factoryDataExchange, vanExchange} {
		if err := ch.ExchangeDeclare(
			ex, "topic",
			true,  // durable
			false, // auto-delete
			false, // internal
			false, // no-wait
			nil,
		); err != nil {
			conn.Close()
			return fmt.Errorf("exchange declare %q: %w", ex, err)
		}
	}

	p.conn = conn
	p.channel = ch
	return nil
}

func (p *Publisher) publish(exchange, routingKey string, v any) error {
	body, err := json.Marshal(v)
	if err != nil {
		return fmt.Errorf("marshal: %w", err)
	}

	p.mu.Lock()
	defer p.mu.Unlock()

	msg := amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Priority:     1, // must be non-zero so the field is included in the AMQP frame
		Body:         body,
	}
	err = p.channel.Publish(exchange, routingKey, false, false, msg)
	if err != nil {
		log.Printf("publish failed (%v), reconnecting...", err)
		p.reconnectLocked()
		// retry once after reconnect
		return p.channel.Publish(exchange, routingKey, false, false, msg)
	}
	return nil
}

// reconnectLocked must be called with p.mu held.
func (p *Publisher) reconnectLocked() {
	if p.channel != nil {
		_ = p.channel.Close()
	}
	if p.conn != nil {
		_ = p.conn.Close()
	}
	for {
		if err := p.connect(); err == nil {
			log.Println("reconnected to RabbitMQ")
			return
		}
		log.Println("reconnect failed, retrying in 5s...")
		time.Sleep(5 * time.Second)
	}
}

func (p *Publisher) close() {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.channel != nil {
		_ = p.channel.Close()
	}
	if p.conn != nil {
		_ = p.conn.Close()
	}
}
