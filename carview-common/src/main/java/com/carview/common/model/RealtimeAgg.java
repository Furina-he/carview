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
 * Flink 实时聚合结果（对应 realtime_agg_10s 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realtime_agg_10s")
public class RealtimeAgg implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer onlineCount;
    private Double avgSpeed;
    private Double totalMileageIncrement;
    private Double avgFuelConsumption;
    private Integer alarmCount;
    private LocalDateTime createdAt;
}
