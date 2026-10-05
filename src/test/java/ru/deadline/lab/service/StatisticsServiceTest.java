package ru.deadline.lab.service;

import org.junit.jupiter.api.Test;
import org.apache.commons.math3.distribution.BinomialDistribution;
import ru.deadline.lab.model.SurveyResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class StatisticsServiceTest {
    private final StatisticsService statistics = new StatisticsService();

    @Test
    void knownOutcomeDenominatorExcludesPendingAndUnknownSubmissions() {
        var result = statistics.summarize(List.of(row("ON_TIME"), row("LATE"), row("NOT_SUBMITTED"), row("UNKNOWN")));

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.known()).isEqualTo(2);
        assertThat(result.onTime()).isEqualTo(1);
        assertThat(result.late()).isEqualTo(1);
        assertThat(result.pending()).isEqualTo(1);
        assertThat(result.unknown()).isEqualTo(1);
        assertThat(result.onTimePercent()).isEqualTo(50);
        assertThat(result.notOnTimePercent()).isEqualTo(50);
    }

    @Test
    void emptyOrUnknownOnlyDataNeverProducesNaN() {
        assertThat(statistics.summarize(List.of()).onTimePercent()).isZero();
        var result = statistics.summarize(List.of(row("UNKNOWN"), row("NOT_SUBMITTED")));
        assertThat(result.known()).isZero();
        assertThat(result.notOnTimePercent()).isZero();
    }

    @Test
    void symmetricBinomialTailAndExpectedCountAreCorrect() {
        var result = statistics.simulate(5, 0.5, 10_000, 3, 42L);

        assertThat(result.expected()).isEqualTo(2.5);
        assertThat(result.theoreticalTail()).isCloseTo(0.5, within(1e-12));
        assertThat(result.empiricalTail()).isCloseTo(0.5, within(0.025));
        assertThat(result.histogram()).hasSize(6);
        assertThat(result.histogram().stream().mapToInt(bin -> bin.observed()).sum()).isEqualTo(10_000);
        assertThat(result.histogram().stream().mapToDouble(bin -> bin.expectedCount()).sum())
                .isCloseTo(10_000, within(1e-8));
    }

    @Test
    void oneKnownExactTailProbabilityMatchesHandCalculation() {
        // P(X >= 4) for five fair independent trials = (5 + 1) / 32.
        var result = statistics.simulate(5, 0.5, 100, 4, 20);
        assertThat(result.theoreticalTail()).isCloseTo(6.0 / 32, within(1e-12));
    }

    @Test
    void simulationCanBeRepeatedExactlyWithItsSeed() {
        assertThat(statistics.simulate(20, 0.3, 1000, 5, 875L))
                .isEqualTo(statistics.simulate(20, 0.3, 1000, 5, 875L));
    }

    @Test
    void oneStudentAndOneRunHaveFiniteExactValues() {
        var result = statistics.simulate(1, 0.375, 1, 1, 13);
        var grouped = statistics.groupSimulation(result);

        assertThat(result.theoreticalTail()).isEqualTo(0.375);
        assertThat(result.histogram()).hasSize(2);
        assertThat(result.firstGroup()).hasSize(1);
        assertThat(result.firstGroup().stream().filter(Boolean::booleanValue).count()).isEqualTo(result.firstResult());
        assertThat(result.mean()).isEqualTo(result.firstResult());
        assertThat(result.convergence()).hasSize(1);
        assertThat(result.convergence().getFirst().runs()).isEqualTo(1);
        assertThat(result.convergence().getFirst().empiricalProbability()).isEqualTo(result.empiricalTail());
        assertThat(grouped.deviation()).isNull();
    }

    @Test
    void maximumRunCountProvidesNormalizedProbabilitiesAndAccurateMean() {
        var result = statistics.simulate(20, 0.62, 50_000, 12, 125);

        assertThat(result.histogram().stream().mapToInt(bin -> bin.observed()).sum()).isEqualTo(50_000);
        assertThat(result.histogram().stream().mapToDouble(bin -> bin.empiricalProbability()).sum())
                .isCloseTo(1, within(1e-12));
        assertThat(result.histogram().stream().mapToDouble(bin -> bin.theoreticalProbability()).sum())
                .isCloseTo(1, within(1e-12));
        assertThat(result.mean()).isCloseTo(result.expected(), within(0.05));
        assertThat(result.empiricalTail()).isCloseTo(result.theoreticalTail(), within(0.01));
        assertThat(result.absoluteTailDifference()).isEqualTo(Math.abs(result.theoreticalTail() - result.empiricalTail()));
        assertThat(result.convergence().getLast().runs()).isEqualTo(50_000);
        assertThat(result.convergence().getLast().empiricalProbability()).isEqualTo(result.empiricalTail());
    }

    @Test
    void convergencePointsUseTheSameSimulationSequenceRatherThanIndependentReruns() {
        var result = statistics.simulate(15, 0.4, 2500, 6, 775);

        for (int index : List.of(0, 10, result.convergence().size() / 2, result.convergence().size() - 1)) {
            var checkpoint = result.convergence().get(index);
            var prefix = statistics.simulate(15, 0.4, checkpoint.runs(), 6, 775);
            assertThat(checkpoint.empiricalProbability()).isEqualTo(prefix.empiricalTail());
        }
        assertThat(result.convergence()).extracting(point -> point.runs()).isSorted();
    }

    @Test
    void theoreticalMassAndTailMatchApacheCommonsMathAcrossEdgeThresholds() {
        for (int n : List.of(1, 7, 100)) {
            for (double p : List.of(0.0, 0.17, 0.5, 1.0)) {
                var theory = new BinomialDistribution(n, p);
                for (int threshold : List.of(0, n)) {
                    var result = statistics.simulate(n, p, 3, threshold, 9);
                    double expectedTail = threshold == 0 ? 1 : theory.probability(n);
                    assertThat(result.theoreticalTail()).isCloseTo(expectedTail, within(1e-12));
                    result.histogram().forEach(bin -> assertThat(bin.theoreticalProbability())
                            .isCloseTo(theory.probability(bin.value()), within(1e-14)));
                }
            }
        }
    }

    @Test
    void singleGroupExperimentIsIndependentAndReproducibleAtProbabilityBoundaries() {
        var zero = statistics.simulateGroup(100, 0, 19);
        var one = statistics.simulateGroup(100, 1, 19);
        var ordinary = statistics.simulateGroup(20, 0.62, 19);

        assertThat(zero.successes()).isZero();
        assertThat(zero.outcomes()).containsOnly(false);
        assertThat(one.successes()).isEqualTo(100);
        assertThat(one.outcomes()).containsOnly(true);
        assertThat(ordinary).isEqualTo(statistics.simulateGroup(20, 0.62, 19));
        assertThat(ordinary.outcomes().stream().filter(Boolean::booleanValue).count()).isEqualTo(ordinary.successes());
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulateGroup(0, 0.5, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulateGroup(1, Double.NaN, 1));
    }

    @Test
    void boundaryProbabilitiesProduceDeterministicOutcomes() {
        var zero = statistics.simulate(20, 0, 100, 1, 42);
        var one = statistics.simulate(20, 1, 100, 20, 42);
        var zeroThreshold = statistics.simulate(20, 0, 100, 0, 42);

        assertThat(zero.firstResult()).isZero();
        assertThat(zero.theoreticalTail()).isZero();
        assertThat(zero.empiricalTail()).isZero();
        assertThat(one.firstResult()).isEqualTo(20);
        assertThat(one.theoreticalTail()).isEqualTo(1);
        assertThat(one.empiricalTail()).isEqualTo(1);
        assertThat(zeroThreshold.theoreticalTail()).isEqualTo(1);
        assertThat(zeroThreshold.empiricalTail()).isEqualTo(1);
    }

    @Test
    void simulationRejectsInvalidAndExcessiveInputs() {
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(0, 0.3, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(101, 0.3, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, Double.NaN, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, Double.POSITIVE_INFINITY, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, -0.1, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, 1.1, 100, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, 0.3, 0, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, 0.3, 50_001, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, 0.3, 100, -1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> statistics.simulate(20, 0.3, 100, 21, 1));
    }

    private SurveyResponse row(String status) {
        return new SurveyResponse(status, Instant.parse("2026-09-01T08:00:00Z"), true, "CODE", "THREE_FOUR",
                "ONE", status, "NO", "MENTAL", 3, "TWO");
    }
}
