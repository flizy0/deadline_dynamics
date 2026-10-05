import test from "node:test";
import assert from "node:assert/strict";
import probability from "../public/assets/js/probability.js";

const closeTo = (actual, expected, tolerance = 1e-12) => assert.ok(Math.abs(actual - expected) <= tolerance, `${actual} is not within ${tolerance} of ${expected}`);

test("endpoint probabilities produce point-mass distributions", () => {
  assert.deepEqual(probability.binomialDistribution(4, 0), [1, 0, 0, 0, 0]);
  assert.deepEqual(probability.binomialDistribution(4, 1), [0, 0, 0, 0, 1]);
});

test("n=1 and a standard n=4 case match the binomial formula", () => {
  const oneTrial = probability.binomialDistribution(1, 0.3);
  closeTo(oneTrial[0], 0.7);
  closeTo(oneTrial[1], 0.3);
  const fairFour = probability.binomialDistribution(4, 0.5);
  [1 / 16, 4 / 16, 6 / 16, 4 / 16, 1 / 16].forEach((value, index) => closeTo(fairFour[index], value));
});

test("theoretical probabilities stay finite and sum to one near both boundaries", () => {
  for (const p of [Number.EPSILON, 0.01, 0.5, 0.739, 0.99, 1 - Number.EPSILON]) {
    const values = probability.binomialDistribution(100, p);
    assert.equal(values.length, 101);
    assert.ok(values.every(Number.isFinite));
    closeTo(values.reduce((sum, value) => sum + value, 0), 1, 1e-10);
  }
});

test("tail thresholds zero and n have the expected endpoint behavior", () => {
  const noMinimum = probability.simulateBinomial({ n: 1, p: 0, runs: 7, threshold: 0, seed: 4 });
  assert.equal(noMinimum.theoreticalTail, 1);
  assert.equal(noMinimum.simulatedTail, 1);
  const allMustSucceed = probability.simulateBinomial({ n: 1, p: 1, runs: 7, threshold: 1, seed: 4 });
  assert.equal(allMustSucceed.theoreticalTail, 1);
  assert.equal(allMustSucceed.simulatedTail, 1);
  const impossible = probability.simulateBinomial({ n: 1, p: 0, runs: 7, threshold: 1, seed: 4 });
  assert.equal(impossible.theoreticalTail, 0);
  assert.equal(impossible.simulatedTail, 0);
});

test("small run counts and fixed seeds produce finite, reproducible results", () => {
  const input = { n: 20, p: 0.5, runs: 1, threshold: 12, seed: 123 };
  const first = probability.simulateBinomial(input);
  const second = probability.simulateBinomial(input);
  assert.deepEqual(first.histogram, second.histogram);
  assert.equal(first.average, second.average);
  assert.equal(first.simulatedTail, second.simulatedTail);
  assert.equal(first.histogram.reduce((sum, count) => sum + count, 0), 1);
  assert.ok([first.average, first.theoreticalTail, first.simulatedTail, ...first.empirical].every(Number.isFinite));
});

test("large run counts remain bounded and approach the theoretical tail", () => {
  const result = probability.simulateBinomial({ n: 100, p: 0.739, runs: 50000, threshold: 74, seed: 987654321 });
  assert.equal(result.histogram.reduce((sum, count) => sum + count, 0), 50000);
  assert.ok(result.histogram.every(Number.isInteger));
  assert.ok([result.average, result.theoreticalTail, result.simulatedTail, ...result.empirical, ...result.checkpoints.map(point => point.y)].every(Number.isFinite));
  assert.ok(Math.abs(result.theoreticalTail - result.simulatedTail) < 0.02);
});

test("invalid simulation bounds are rejected instead of producing NaN", () => {
  assert.throws(() => probability.binomialDistribution(0, 0.5), RangeError);
  assert.throws(() => probability.binomialDistribution(5, 1.1), RangeError);
  assert.throws(() => probability.simulateBinomial({ n: 2, p: 0.5, runs: 0, threshold: 1, seed: 1 }), RangeError);
  assert.throws(() => probability.simulateBinomial({ n: 2, p: 0.5, runs: 10, threshold: 3, seed: 1 }), RangeError);
});
