package com.taskflow.api.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    // 1. Nombres constantes (se utilizaran  en el productor y consumidor)
    public static final String INVITATION_QUEUE = "project.invitation.email.queue";
    public static final String PROJECT_EXCHANGE = "project.exchange";
    public static final String INVITATION_ROUTING_KEY = "project.invitation.email.routing.key";

    // 2. Definimos la cola ("file de espera")
    // El 'true' significa que la cola es "durable" (sobrevive si RabbitMQ se reinicia)
    @Bean
    public Queue projectInvitationQueue() {
        return new Queue(INVITATION_QUEUE, true);
    }

    // 3. Definimos el Exchange (El enrutador)
    // permite reglas de enrutamiento flexibles por si luego agregas más colas
    @Bean
    public TopicExchange projectExchange() {
        return new TopicExchange(PROJECT_EXCHANGE);
    }

    // 4. Creamos el Binding (regla que une al enrutador con la cola)
    // Le decimos: "Todo lo que llegue al Exchange con esta Routing Key, mándalo a esta Cola"
    @Bean
    public Binding binding(Queue invitationQueue, TopicExchange projectExchange) {
        return BindingBuilder.bind(invitationQueue).to(projectExchange).with(INVITATION_ROUTING_KEY);
    }

    // 5. El Traductor (VITAL)
    // Spring por defecto manda objetos Java serializados (ilegibles).
    // Esto fuerza a que los mensajes viajen como JSON puro y limpio.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }


}
