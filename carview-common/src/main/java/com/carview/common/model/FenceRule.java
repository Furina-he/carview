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
 * 电子围栏规则（对应 fence_rule 表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fence_rule")
public class FenceRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fenceName;

    /** 围栏类型: RECTANGLE / CIRCLE */
    private String fenceType;

    // 矩形围栏
    private Double minLng;
    private Double minLat;
    private Double maxLng;
    private Double maxLat;

    // 圆形围栏
    private Double centerLng;
    private Double centerLat;
    private Double radius;

    /** 是否启用 1-启用 0-禁用 */
    private Integer enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
