"use strict";

(function attachProbabilityCore(root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  else root.DeadlineProbability = api;
})(typeof globalThis === "object" ? globalThis : this, function createProbabilityCore() {
  function logFactorials(n) {
    const values = new Array(n + 1).fill(0);
    for (let i = 2; i <= n; i += 1) values[i] = values[i - 1] + Math.log(i);
    return values;
  }

  function binomialDistribution(n, p) {
    if (!Number.isInteger(n) || n < 1 || n > 100 || !Number.isFinite(p) || p < 0 || p > 1) {
      throw new RangeError("Expected 1 <= n <= 100 and 0 <= p <= 1.");
    }
    if (p === 0) return Array.from({ length: n + 1 }, (_, k) => k === 0 ? 1 : 0);
    if (p === 1) return Array.from({ length: n + 1 }, (_, k) => k === n ? 1 : 0);
    const factorials = logFactorials(n);
    const logProbabilities = Array.from({ length: n + 1 }, (_, k) => factorials[n] - factorials[k] - factorials[n - k] + k * Math.log(p) + (n - k) * Math.log1p(-p));
    const largest = Math.max(...logProbabilities);
    const scaled = logProbabilities.map(value => Math.exp(value - largest));
    const sum = scaled.reduce((total, value) => total + value, 0);
    return scaled.map(value => value / sum);
  }

  function seededRandom(seed) {
    let state = seed >>> 0;
    return () => {
      state += 0x6D2B79F5;
      let value = state;
      value = Math.imul(value ^ (value >>> 15), value | 1);
      value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
      return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
    };
  }

  function simulateBinomial({ n, p, runs, threshold, seed }) {
    if (!Number.isInteger(runs) || runs < 1 || runs > 50000) throw new RangeError("Expected 1 <= runs <= 50000.");
    if (!Number.isInteger(threshold) || threshold < 0 || threshold > n) throw new RangeError("Threshold must be between 0 and n.");
    const theory = binomialDistribution(n, p);
    const random = seededRandom(seed);
    const histogram = new Array(n + 1).fill(0);
    const checkpoints = [];
    const stride = Math.max(1, Math.floor(runs / 60));
    let total = 0;
    let tailHits = 0;
    let cumulativeHits = 0;

    for (let run = 1; run <= runs; run += 1) {
      let successes = 0;
      for (let student = 0; student < n; student += 1) if (random() < p) successes += 1;
      histogram[successes] += 1;
      total += successes;
      if (successes >= threshold) {
        tailHits += 1;
        cumulativeHits += 1;
      }
      if (run % stride === 0 || run === runs) checkpoints.push({ x: run, y: cumulativeHits / run });
    }

    const empirical = histogram.map(count => count / runs);
    return {
      theory,
      histogram,
      empirical,
      theoreticalTail: theory.slice(threshold).reduce((sum, value) => sum + value, 0),
      simulatedTail: tailHits / runs,
      average: total / runs,
      checkpoints,
      random
    };
  }

  return Object.freeze({ binomialDistribution, seededRandom, simulateBinomial });
});
