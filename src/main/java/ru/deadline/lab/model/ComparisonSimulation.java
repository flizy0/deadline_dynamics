package ru.deadline.lab.model;

public record ComparisonSimulation(LabScenario earlierScenario, LabScenario finalScenario,
                                   SimulationResult earlier, SimulationResult finalDay,
                                   double overlap, double expectedDifference) { }
