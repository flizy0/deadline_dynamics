package ru.deadline.lab.model;

import java.util.List;

public record SimulationResult(int size, double probability, int runs, int threshold, long seed,
                               double expected, double theoreticalTail, double empiricalTail,
                               int firstResult, List<HistogramBin> histogram, double mean,
                               double absoluteTailDifference, List<ConvergencePoint> convergence,
                               List<Boolean> firstGroup) {
    public SimulationResult {
        histogram = List.copyOf(histogram);
        convergence = List.copyOf(convergence);
        firstGroup = List.copyOf(firstGroup);
    }

    public double absoluteDifference() {
        return absoluteTailDifference;
    }
}
