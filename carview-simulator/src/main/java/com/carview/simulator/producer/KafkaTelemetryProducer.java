package com.carview.simulator.producer;

import com.carview.common.constants.KafkaConstants;
import com.carview.common.model.VehicleTelemetry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Kafka 生产者：发送车辆遥测数据
 */
public class KafkaTelemetryProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaTelemetryProducer.class);
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaTelemetryProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
    }

    public void send(VehicleTelemetry telemetry) {
        try {
            String json = objectMapper.writeValueAsString(telemetry);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConstants.TOPIC_VEHICLE_DATA,
                    telemetry.getVehicleId(),
                    json
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("发送消息失败: vehicle={}", telemetry.getVehicleId(), exception);
                }
            });
        } catch (Exception e) {
            log.error("序列化消息失败: vehicle={}", telemetry.getVehicleId(), e);
        }
    }

    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
