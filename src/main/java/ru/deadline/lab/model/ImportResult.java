package ru.deadline.lab.model;

public record ImportResult(int inserted, int updated, int unchanged, int received) {
}
