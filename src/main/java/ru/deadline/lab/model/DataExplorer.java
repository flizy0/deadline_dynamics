package ru.deadline.lab.model;

import java.util.List;
import ru.deadline.lab.service.StatisticsService.FrequencyTable;

public record DataExplorer(String field, String analysis, String view, String recommendedView,
                           List<String> views, FrequencyTable frequency, List<OutcomeRow> outcomes,
                           List<String> highestCodes, Double highestPercent) { }
