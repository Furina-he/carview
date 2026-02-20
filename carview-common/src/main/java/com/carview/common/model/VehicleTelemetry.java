package com.carview.common.model;

import com.carview.common.enums.DriverBehavior;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 车辆遥测数据（与 Kafka 消息结构对应）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTelemetry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 车牌号 */
    private String vehicleId;

    /** 车辆识别码 */
    private String vin;

    /** 时间戳（毫秒） */
    private Long timestamp;

    /** 经度 */
    private Double lng;

    /** 纬度 */
    private Double lat;

    /** 速度(km/h) */
    private Double speed;

    /** 转速(rpm) */
    private Integer rpm;

    /** 瞬时油耗(L/100km) */
    private Double fuelConsumption;

    /** 总里程(km) */
    private Double mileage;

    /** 发动机温度(℃) */
    private Integer engineTemp;

    /** 电池电压(V) */
    private Double batteryVoltage;

    /** 故障码(OBD) */
    private String faultCode;

    /** 驾驶行为标签 */
    private String driverBehavior;
}
