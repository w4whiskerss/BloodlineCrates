package com.bloodlinecrates.model;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum LeaderboardPeriod {
    DAILY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date;
        }
    },
    WEEKLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.with(java.time.DayOfWeek.MONDAY);
        }
    },
    MONTHLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.withDayOfMonth(1);
        }
    },
    YEARLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.with(TemporalAdjusters.firstDayOfYear());
        }
    };

    public abstract LocalDate periodStart(LocalDate date);
}
