package com.yu.yupicture.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class TimeUtil {

    // 时间格式化器（用于前端展示）
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CYCLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 处理时间范围：若前端未传开始/结束时间，默认使用当月时间范围
     * @param startTime 前端传入的开始时间（可能为null）
     * @param endTime 前端传入的结束时间（可能为null）
     * @return 处理后的时间范围 [start, end]
     */
    public LocalDateTime[] handleTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime actualStart;
        LocalDateTime actualEnd;

        // 处理开始时间（默认当月1日0点）
        if (startTime == null) {
            LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
            actualStart = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN);
        } else {
            actualStart = startTime;
        }

        // 处理结束时间（默认当月最后1日23:59:59）
        if (endTime == null) {
            LocalDate lastDayOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            actualEnd = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX);
        } else {
            actualEnd = endTime;
        }

        // 校验时间范围合法性
        if (actualStart.isAfter(actualEnd)) {
            log.warn("时间范围不合法，自动交换 start={}, end={}", actualStart, actualEnd);
            LocalDateTime temp = actualStart;
            actualStart = actualEnd;
            actualEnd = temp;
        }

        return new LocalDateTime[]{actualStart, actualEnd};
    }

    /**
     * 获取上月时间范围（用于定时任务生成上月报告）
     * @return 上月时间范围 [上月1日0点, 上月最后1日23:59:59]
     */
    public LocalDateTime[] getLastMonthRange() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = today.minusMonths(1).withDayOfMonth(firstDayOfLastMonth.lengthOfMonth());

        return new LocalDateTime[]{
                LocalDateTime.of(firstDayOfLastMonth, LocalTime.MIN),
                LocalDateTime.of(lastDayOfLastMonth, LocalTime.MAX)
        };
    }

    /**
     * 格式化时间为字符串（yyyy-MM-dd HH:mm:ss）
     */
    public String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME_FORMATTER);
    }

    /**
     * 格式化时间范围为友好字符串（如：2025-09-01至2025-09-30）
     */
    public String formatRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "";
        }
        // 若为同一月份，简化显示（如：2025-09-01至30）
        if (start.getYear() == end.getYear() && start.getMonth() == end.getMonth()) {
            return String.format("%s-%02d-%02d至%02d",
                    start.getYear(), start.getMonthValue(), start.getDayOfMonth(), end.getDayOfMonth());
        }
        return start.format(DATE_FORMATTER) + "至" + end.format(DATE_FORMATTER);
    }

    /**
     * 获取周期字符串（如：2025-09）
     */
    public String getCycle(LocalDateTime time) {
        return time == null ? "" : time.format(CYCLE_FORMATTER);
    }
}
