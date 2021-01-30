package cloud.apposs.util;

import java.sql.Timestamp;
import java.util.Calendar;

public class Parser {
    public static Integer parseInt(String value) {
        return parseInt(value, 0);
    }

    public static Integer parseInt(String value, int defaultValue) {
        try {
            if (StrUtil.isEmpty(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Boolean parseBoolean(String value) {
        return parseBoolean(value, false);
    }

    public static Boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        value = value.trim().toLowerCase();
        if (value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("on") || value.equals("1")) {
            return true;
        } else if (value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("off") || value.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    public static Long parseLong(String value) {
        return parseLong(value, 0L);
    }

    public static Long parseLong(String value, long defaultValue) {
        try {
            if (StrUtil.isEmpty(value)) {
                return defaultValue;
            }
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Double parseDouble(String value) {
        return parseDouble(value, 0.0);
    }

    public static Double parseDouble(String value, double defaultValue) {
        try {
            if (StrUtil.isEmpty(value)) {
                return defaultValue;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Float parseFloat(String value) {
        return parseFloat(value, 0.0f);
    }

    public static Float parseFloat(String value, float defaultValue) {
        try {
            if (StrUtil.isEmpty(value)) {
                return defaultValue;
            }
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Short parseShort(String value) {
        return parseShort(value, (short) 0);
    }

    public static Short parseShort(String value, short defaultValue) {
        try {
            if (StrUtil.isEmpty(value)) {
                return defaultValue;
            }
            return Short.parseShort(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 日期转换为时间戳
     *
     * @param  calendar 日期
     * @return 时间戳
     */
    public static Timestamp parseTimestamp(Calendar calendar) {
        return new Timestamp(calendar.getTimeInMillis());
    }

    /**
     * 时间戳转换为日期
     *
     * @param  timestamp 时间戳
     * @return 日期
     */
    public static Calendar parseCalendar(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getTime());
        return calendar;
    }

    /**
     * 时间戳转换为日期
     *
     * @param  date 时间戳
     * @return 日期
     */
    public static Calendar parseCalendar(java.sql.Date date) {
        if (date == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar;
    }

    /**
     * 时间戳转换为日期
     *
     * @param  timestamp 时间戳
     * @return 日期
     */
    public static Calendar parseCalendar(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar;
    }
}
