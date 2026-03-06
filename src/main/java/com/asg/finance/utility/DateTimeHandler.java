package com.asg.finance.utility;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeHandler {
    public static LocalDateTime convertDateTime(LocalDateTime date) {
        if (date == null)
            return DateUtil.getCurrentDateTimeInUserTimeZone();
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.of(date, zoneId).toLocalDateTime();
    }

    public static LocalDate convertDate(LocalDateTime date) {
        if (date == null)
            return DateUtil.getCurrentDateInUserTimeZone();
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.of(date, zoneId).toLocalDate();
    }
}
