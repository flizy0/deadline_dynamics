package ru.deadline.lab.model;

import java.util.List;

public record SecondaryFinding(String field, List<String> codes, double onTimePercent, int known) { }
