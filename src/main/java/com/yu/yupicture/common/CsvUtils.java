package com.yu.yupicture.common;

import com.yu.yupicture.modle.entity.SystemMonitorDoc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CsvUtils {
    // 日期格式化（将时间戳转换为可读格式）
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 将监控数据列表转换为CSV字符串（隐藏数据库字段，合并数值与单位）
     */
    public static String convertMonitorDataToCsv(List<SystemMonitorDoc> dataList) {
        // CSV表头（中文友好，包含单位）
        String header = "采集时间," +
                "CPU使用率(%)," +
                "系统总内存(MB),系统已用内存(MB),内存使用率(%)," +
                "磁盘总容量(GB),磁盘已用容量(GB),磁盘使用率(%)," +
                "JVM总内存(MB),JVM已用内存(MB),JVM使用率(%)," +
                "下载速度(MB/s),上传速度(MB/s)," +
                "是否触发预警,预警信息\n";

        // 转换数据行
        String content = dataList.stream()
                .map(doc -> {
                    // 计算各使用率（保留两位小数）
                    double memoryUsage = doc.getSysMemTotal() > 0 ?
                            (double) doc.getSysMemUsed() / doc.getSysMemTotal() * 100 : 0;
                    double diskUsage = doc.getDiskTotal() > 0 ?
                            doc.getDiskUsed() / doc.getDiskTotal() * 100 : 0;
                    double jvmUsage = doc.getJvmMax() > 0 ?
                            (double) doc.getJvmUsed() / doc.getJvmMax() * 100 : 0;

                    // 拼接一行数据（处理特殊字符和格式）
                    return String.join(",",
                            DATE_FORMAT.format(new Date(doc.getMetricTimestamp())), // 采集时间
                            String.format("%.2f", doc.getCpuValue()), // CPU使用率

                            // 系统内存相关
                            String.valueOf(doc.getSysMemTotal()),
                            String.valueOf(doc.getSysMemUsed()),
                            String.format("%.2f", memoryUsage),

                            // 磁盘相关
                            String.format("%.2f", doc.getDiskTotal()),
                            String.format("%.2f", doc.getDiskUsed()),
                            String.format("%.2f", diskUsage),

                            // JVM相关
                            String.valueOf(doc.getJvmMax()),
                            String.valueOf(doc.getJvmUsed()),
                            String.format("%.2f", jvmUsage),

                            // 网络速度
                            String.format("%.2f", doc.getNetDownloadSpeed()),
                            String.format("%.2f", doc.getNetUploadSpeed()),

                            // 预警信息
                            doc.isHasWarning() ? "是" : "否",
                            wrapCsvField(doc.getWarningMsg()) // 处理含逗号的字段
                    );
                })
                .collect(Collectors.joining("\n"));

        return header + content;
    }

    /**
     * 处理CSV字段中的特殊字符（逗号、双引号等）
     */
    private static String wrapCsvField(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }
        // 若字段包含逗号、双引号或换行符，用双引号包裹（避免CSV格式错乱）
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\""); // 双引号转义为两个双引号
            return "\"" + field + "\"";
        }
        return field;
    }
}
