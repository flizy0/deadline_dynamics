"use strict";

// Aggregate-only snapshot from the Google Forms workbook. No timestamps or row-level answers are published.
window.STUDY_DATA = Object.freeze({
  sourceResponses: 52,
  eligible: 29,
  ineligible: 23,
  known: 23,
  onTime: 17,
  late: 6,
  pending: 6,
  collectedFrom: "30.09.2026",
  collectedTo: "02.10.2026",
  variables: [
    {
      key: "allotted",
      categories: [
        { code: "sameDay", count: 5, onTime: 4, late: 0, pending: 1 },
        { code: "oneDay", count: 2, onTime: 0, late: 2, pending: 0 },
        { code: "twoDays", count: 3, onTime: 2, late: 0, pending: 1 },
        { code: "threeFourDays", count: 6, onTime: 4, late: 2, pending: 0 },
        { code: "fiveSevenDays", count: 9, onTime: 5, late: 1, pending: 3 },
        { code: "eightPlusDays", count: 2, onTime: 1, late: 0, pending: 1 },
        { code: "dontRemember", count: 2, onTime: 1, late: 1, pending: 0 }
      ]
    },
    {
      key: "start",
      categories: [
        { code: "eightPlusDays", count: 2, onTime: 2, late: 0, pending: 0 },
        { code: "fiveSevenDays", count: 1, onTime: 1, late: 0, pending: 0 },
        { code: "threeFourDays", count: 8, onTime: 3, late: 2, pending: 3 },
        { code: "twoDays", count: 7, onTime: 6, late: 1, pending: 0 },
        { code: "oneDay", count: 5, onTime: 2, late: 2, pending: 1 },
        { code: "dueDate", count: 3, onTime: 1, late: 0, pending: 2 },
        { code: "afterDeadline", count: 1, onTime: 1, late: 0, pending: 0 },
        { code: "notStarted", count: 1, onTime: 0, late: 1, pending: 0 },
        { code: "dontRemember", count: 1, onTime: 1, late: 0, pending: 0 }
      ]
    },
    {
      key: "outcome",
      categories: [
        { code: "onTime", count: 17 },
        { code: "late", count: 6 },
        { code: "pending", count: 6 }
      ]
    },
    {
      key: "extension",
      categories: [
        { code: "no", count: 14, onTime: 8, late: 2, pending: 4 },
        { code: "yes", count: 10, onTime: 5, late: 4, pending: 1 },
        { code: "unknown", count: 5, onTime: 4, late: 0, pending: 1 }
      ]
    },
    {
      key: "planning",
      categories: [
        { code: "mentalPlan", count: 20, onTime: 12, late: 3, pending: 5 },
        { code: "noPlan", count: 7, onTime: 4, late: 3, pending: 0 },
        { code: "writtenPlan", count: 1, onTime: 1, late: 0, pending: 0 },
        { code: "notStarted", count: 1, onTime: 0, late: 0, pending: 1 }
      ]
    },
    {
      key: "difficulty",
      categories: [
        { code: "veryEasy", count: 1, onTime: 1, late: 0, pending: 0 },
        { code: "ratherEasy", count: 3, onTime: 1, late: 2, pending: 0 },
        { code: "moderate", count: 17, onTime: 10, late: 4, pending: 3 },
        { code: "ratherDifficult", count: 6, onTime: 5, late: 0, pending: 1 },
        { code: "veryDifficult", count: 2, onTime: 0, late: 0, pending: 2 }
      ]
    },
    {
      key: "otherDeadlines",
      categories: [
        { code: "none", count: 2, onTime: 2, late: 0, pending: 0 },
        { code: "one", count: 3, onTime: 1, late: 2, pending: 0 },
        { code: "two", count: 9, onTime: 5, late: 2, pending: 2 },
        { code: "three", count: 6, onTime: 4, late: 1, pending: 1 },
        { code: "fourPlus", count: 9, onTime: 5, late: 1, pending: 3 }
      ]
    }
  ]
});
