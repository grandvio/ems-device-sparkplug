package com.ems.sparkplug.model;
import lombok.Data;

@Data
public class SpbMetricDto {
    private String groupId;
    private String edgeNodeId;
    private String deviceId;
    private String metricName;
    /** 报文内原始时间戳（毫秒） */
    private Long ts;
    /** 原始值对象，后面判断类型 */
    private Object value;
    /** 质量码 */
    private Integer quality;
}
