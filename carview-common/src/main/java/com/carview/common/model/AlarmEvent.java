package com.carview.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警事件（对应 alarm_event 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("alarm_event")
public class AlarmEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String vehicleId;

    /** 告警类型: OVERSPEED / FENCE_OUT / FAULT / ENGINE_OVERHEAT / LOW_BATTERY */
    private String alarmType;

    /** 告警级别: 1-严重 2-警告 3-提示 */
    private Integer alarmLevel;

    /** 状态: 1-活跃 2-已确认 3-已解决 */
    private Integer alarmStatus;

    private String alarmMessage;
    private String alarmValue;
    private Double lng;
    private Double lat;
    private LocalDateTime eventTime;
    private LocalDateTime ackTime;
    private LocalDateTime createdAt;
}
