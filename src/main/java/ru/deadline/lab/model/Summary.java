package ru.deadline.lab.model;

public record Summary(int total, int known, int onTime, int late, int pending, int unknown,
                      double onTimePercent, double notOnTimePercent) {
}
