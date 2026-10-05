package ru.deadline.lab.model;

public record HistogramBin(int value, int observed, double expectedCount,
                           double empiricalProbability, double theoreticalProbability) {
}
