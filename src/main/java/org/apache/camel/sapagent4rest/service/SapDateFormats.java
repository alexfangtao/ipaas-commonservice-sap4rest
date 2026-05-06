package org.apache.camel.sapagent4rest.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SAP 日期/时间字段的格式映射，按 SAP 字段长度选择 pattern。
 * 请求解析、响应序列化、preview demo 三处共用，避免规则漂移。
 * <p>
 * length 含义（来自 SAP RFC 元数据 annotation "length"）：
 * 6  → TIME (HHMMSS)
 * 8  → DATE (YYYYMMDD)
 * 10/12/14/17 → 不同精度的时间戳
 */
public class SapDateFormats {
    private SapDateFormats() {
    }

    private static final int LEN_TIME = 6;   // SAP TIME (HHMMSS)
    private static final int LEN_DATE = 8;   // SAP DATE (YYYYMMDD)

    private static final ZoneId ZONE = ZoneId.systemDefault();

    public static String patternByLength(int length) {
        switch (length) {
            case 6:
                return "HH:mm:ss";
            case 10:
                return "yyyy-MM-dd HH";
            case 12:
                return "yyyy-MM-dd HH:mm";
            case 14:
                return "yyyy-MM-dd HH:mm:ss";
            case 17:
                return "yyyy-MM-dd HH:mm:ss.SSS";
            case 8:
            default:
                return "yyyy-MM-dd";
        }
    }

    /**
     * DateTimeFormatter 是不可变 + 线程安全的，可缓存复用
     */
    private static final Map<Integer, DateTimeFormatter> FORMATTERS = new ConcurrentHashMap<>();

    private static DateTimeFormatter formatterFor(int length) {
        return FORMATTERS.computeIfAbsent(length, SapDateFormats::buildFormatter);
    }

    private static DateTimeFormatter buildFormatter(int length) {
        if (length == LEN_DATE) {
            return DateTimeFormatter.ofPattern(patternByLength(length));
        }
        DateTimeFormatterBuilder b = new DateTimeFormatterBuilder()
                .appendPattern(patternByLength(length))
                // pattern 中没有更细字段时给默认值，否则 LocalDateTime.parse 会抛异常
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0);
        if (length == LEN_TIME) {
            // TIME-only 字段给一个稳定的纪元日期
            b.parseDefaulting(ChronoField.YEAR, 1970)
                    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1);
        }
        return b.toFormatter();
    }

    public static String format(Date date, int length) {
        ZonedDateTime zdt = date.toInstant().atZone(ZONE);
        DateTimeFormatter fmt = formatterFor(length);
        if (length == LEN_TIME) {
            return zdt.toLocalTime().format(fmt);
        }
        if (length == LEN_DATE) {
            return zdt.toLocalDate().format(fmt);
        }
        return zdt.toLocalDateTime().format(fmt);
    }

    public static Date parse(String text, int length) {
        DateTimeFormatter fmt = formatterFor(length);
        if (length == LEN_DATE) {
            LocalDate d = LocalDate.parse(text, fmt);
            return Date.from(d.atStartOfDay(ZONE).toInstant());
        }
        // 6 / 10 / 12 / 14 / 17 / 默认 都按 LocalDateTime 走（TIME 已 parseDefaulting 了日期）
        LocalDateTime dt = LocalDateTime.parse(text, fmt);
        return Date.from(dt.atZone(ZONE).toInstant());
    }

    /**
     * preview demo 用的固定时刻：2024-09-09 12:34:56.789
     */
    public static String demo(int length) {
        return format(DEMO_DATE, length);
    }

    private static final Date DEMO_DATE = Date.from(
            LocalDateTime.of(2024, 9, 9, 12, 34, 56, 789_000_000)
                    .atZone(ZONE).toInstant());
}
