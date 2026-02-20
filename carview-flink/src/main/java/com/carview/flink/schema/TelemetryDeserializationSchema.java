package com.carview.flink.schema;

import com.carview.common.model.VehicleTelemetry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.io.IOException;

/**
 * Kafka 消息反序列化：JSON -> VehicleTelemetry
 */
public class TelemetryDeserializationSchema implements DeserializationSchema<VehicleTelemetry> {

    private transient ObjectMapper objectMapper;

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    @Override
    public VehicleTelemetry deserialize(byte[] message) throws IOException {
        return getObjectMapper().readValue(message, VehicleTelemetry.class);
    }

    @Override
    public boolean isEndOfStream(VehicleTelemetry nextElement) {
        return false;
    }

    @Override
    public TypeInformation<VehicleTelemetry> getProducedType() {
        return TypeInformation.of(VehicleTelemetry.class);
    }
}
