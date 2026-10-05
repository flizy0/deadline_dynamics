package ru.deadline.lab.model;

import java.util.List;

public record GroupExperiment(int size, double probability, long seed, int successes,
                              List<Boolean> outcomes) {
    public GroupExperiment {
        outcomes = List.copyOf(outcomes);
    }
}
