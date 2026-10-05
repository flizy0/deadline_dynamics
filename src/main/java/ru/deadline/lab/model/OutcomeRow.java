package ru.deadline.lab.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OutcomeRow(String code, int total, int known, int onTime, int late,
                         int pending, int unknown, Double onTimePercent) {
    @JsonProperty("latePercent")
    public Double latePercent() {
        return known == 0 ? null : 100.0 * late / known;
    }
}
