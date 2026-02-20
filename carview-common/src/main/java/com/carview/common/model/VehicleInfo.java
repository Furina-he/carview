package com.carview.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆基础信息（对应 vehicle_info 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vehicle_info")
public class VehicleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String vehicleId;
    private String vin;
    private String plateNumber;
    private String brand;
    private String model;
    private String color;
    private String ownerName;
    private String ownerPhone;
    private LocalDate registerDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
