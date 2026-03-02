package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String WAREHOUSE_HEARTBEAT_EXCHANGE    = "warehouse.heartbeat";
    public static final String WAREHOUSE_DATA_EXCHANGE         = "warehouse.data";
    public static final String WAREHOUSE_HEARTBEAT_QUEUE       = "warehouse.heartbeat.queue";
    public static final String WAREHOUSE_TEMPERATURE_QUEUE     = "warehouse.temperature.queue";

    @Bean
    TopicExchange warehouseHeartbeatExchange() {
        return new TopicExchange(WAREHOUSE_HEARTBEAT_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange warehouseDataExchange() {
        return new TopicExchange(WAREHOUSE_DATA_EXCHANGE, true, false);
    }

    @Bean
    Queue warehouseHeartbeatQueue() {
        return new Queue(WAREHOUSE_HEARTBEAT_QUEUE, true);
    }

    @Bean
    Queue warehouseTemperatureQueue() {
        return new Queue(WAREHOUSE_TEMPERATURE_QUEUE, true);
    }

    @Bean
    Binding warehouseHeartbeatBinding(Queue warehouseHeartbeatQueue, TopicExchange warehouseHeartbeatExchange) {
        return BindingBuilder.bind(warehouseHeartbeatQueue).to(warehouseHeartbeatExchange).with("warehouse.#");
    }

    @Bean
    Binding warehouseTemperatureBinding(Queue warehouseTemperatureQueue, TopicExchange warehouseDataExchange) {
        return BindingBuilder.bind(warehouseTemperatureQueue).to(warehouseDataExchange).with("warehouse.#");
    }


    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter messageConverter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
