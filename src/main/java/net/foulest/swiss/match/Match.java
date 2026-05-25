/*
 * Swiss - a Monte Carlo bracket simulator for Counter-Strike 2 tournaments.
 * Copyright (C) 2024 Foulest (https://github.com/Foulest)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.foulest.swiss.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.foulest.swiss.team.Team;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a match between two teams.
 *
 * <p><b>BO1</b> and <b>BO5</b> matches use a single win probability derived
 * from {@link #teamScore}, a composite of all six per-player and team-level
 * performance metrics. <b>BO3</b> matches ({@code bestOfX == 2}) simulate a
 * full 7-step opponent-aware map veto, then compute per-map win probabilities
 * using a sample-size-adaptive blend of map-specific win rates, CT/T side
 * splits, and the overall quality signal.
 *
 * <p><b>Team score components</b> (all filtered to top-50 opponents, last
 * 3 months):
 * <ul>
 *   <li><b>Bayesian map win rate (40%)</b>: (mapsWon + 1) / (mapsPlayed + 2).
 *       Laplace-smoothed observed map win rate vs top-50. The primary anchor —
 *       directly measures competitive results without any proxy distortion.</li>
 *   <li><b>Mean round swing (25%)</b>: average per-player impact on round-win
 *       probability per kill. Measures execution quality across every match;
 *       immune to scheduling or opponent selection. Player-level granularity
 *       means one anchor player correctly drags the team mean.</li>
 *   <li><b>Mean trading score (15%)</b>: average per-player trade kill
 *       efficiency. Captures whether kills compound into numerical advantages
 *       or wash out through immediate trading — the gap between kill volume
 *       and round retention that round swing alone does not fully isolate.</li>
 *   <li><b>Mean opening score (10%)</b>: average per-player success rate in
 *       first duels. Opening kills are the highest-leverage moment of any
 *       round; consistent first-duel wins are a distinct skill not fully
 *       captured by aggregate round swing.</li>
 *   <li><b>Mean firepower (6%)</b>: average per-player kill output per round.
 *       Measures raw offensive threat. Kept low to avoid double-counting with
 *       round swing, which captures whether those kills actually convert.</li>
 *   <li><b>Mean HLTV rating (4%)</b>: average of each player's Rating 3.0.
 *       A composite quality floor signal — five above-average raters indicate
 *       depth across all facets of the game.</li>
 * </ul>
 *
 * <p><b>Veto design</b>: all three veto steps use opponent data because
 * pick/ban percentages are publicly available. The first ban is a combined
 * self-avoidance and opponent-strength signal; picks are adjusted by a
 * matchup multiplier that incorporates CT/T side balance; second bans
 * predominantly deny the opponent's best remaining map.
 *
 * <p><b>Per-map probability</b>: blends three signals — overall team quality,
 * map-specific win rate, and CT/T round win rate averages — with confidence
 * determined by per-map sample size. CT/T data is incorporated when both teams
 * have at least three appearances on the map with non-zero side rates, so that
 * a team relying on winning the knife round to secure their dominant side is
 * correctly penalised relative to a team that is strong on both sides.
 *
 * @author Foulest
 */
@Data
@AllArgsConstructor
public class Match {

    private Team team1;
    private Team team2;
    private int bestOfX;

    // Map pool — indices into the seven-map active pool
    private static final int MIRAGE = 0;
    private static final int INFERNO = 1;
    private static final int NUKE = 2;
    private static final int ANCIENT = 3;
    private static final int OVERPASS = 4;
    private static final int ANUBIS = 5;
    private static final int DUST2 = 6;
    private static final int NUM_MAPS = 7;

    /**
     * Simulates a match between the two teams.
     *
     * <ul>
     *   <li><b>BO1</b>: single win probability from {@link #calculateWinProbability}.</li>
     *   <li><b>BO3</b> ({@code bestOfX == 2}): full opponent-aware veto followed
     *       by per-map win probabilities when both teams have ≥ 10 maps played
     *       vs top-50; flat formula otherwise.</li>
     *   <li><b>BO5</b>: single win probability repeated until a team wins 3 maps.</li>
     * </ul>
     *
     * @param mostLikelyOnly If true, return the expected winner deterministically.
     * @return The winning team.
     */
    public Team simulate(boolean mostLikelyOnly) {
        if (bestOfX == 2) {
            if (mostLikelyOnly) {
                return calculateWinProbability(team1, team2) >= 0.5 ? team1 : team2;
            }
            return simulateBO3();
        }

        double p = calculateWinProbability(team1, team2);

        if (mostLikelyOnly) {
            return p >= 0.5 ? team1 : team2;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int t1Wins = 0;
        int t2Wins = 0;

        while (t1Wins < bestOfX && t2Wins < bestOfX) {
            if (rng.nextDouble() < p) {
                t1Wins++;
            } else {
                t2Wins++;
            }
        }
        return t1Wins == bestOfX ? team1 : team2;
    }

    /**
     * Simulates a BO3 series with a full opponent-aware veto and per-map
     * win probabilities. Falls back to the flat formula when one or both
     * teams have fewer than 10 total maps played vs top-50.
     */
    private Team simulateBO3() {
        if (!hasMapData(team1) || !hasMapData(team2)) {
            return simulateBO3Flat();
        }

        int[] maps = simulateVeto(team1, team2);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int t1Wins = 0;
        int t2Wins = 0;

        for (int mapIdx : maps) {
            if (mapIdx < 0 || t1Wins == 2 || t2Wins == 2) {
                break;
            }

            double prob = calculateMapWinProbability(team1, team2, mapIdx);

            if (rng.nextDouble() < prob) {
                t1Wins++;
            } else {
                t2Wins++;
            }
        }
        return t1Wins == 2 ? team1 : team2;
    }

    /**
     * Flat BO3 fallback for teams without sufficient map data.
     */
    private Team simulateBO3Flat() {
        double p = calculateWinProbability(team1, team2);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int t1Wins = 0;
        int t2Wins = 0;

        while (t1Wins < 2 && t2Wins < 2) {
            if (rng.nextDouble() < p) {
                t1Wins++;
            } else {
                t2Wins++;
            }
        }
        return t1Wins == 2 ? team1 : team2;
    }

    // =========================================================================
    // Veto simulation
    //
    // Standard CS2 BO3 veto — 7 steps across a 7-map pool:
    //
    //   1. Team 1 bans  — 55% own avoidance + 45% opponent map strength
    //   2. Team 2 bans  — same
    //   3. Team 1 picks → Map 1  — own pick preference × CT/T-aware matchup multiplier
    //   4. Team 2 picks → Map 2  — same
    //   5. Team 1 bans  — 20% own avoidance + 80% opponent map strength
    //   6. Team 2 bans  — same
    //   7. Remaining    → Map 3 (decider)
    //
    // All percentages are confidence-blended with a uniform prior via per-map
    // sample size before squaring, preventing small-sample extremes from
    // dominating after the squaring amplification step.
    //
    // Opponent strength in ban/pick weights uses effectiveMapStrength, which
    // blends the opponent's overall WR with their CT/T-balanced average round
    // win rate when sufficient side data is available. A team whose WR comes
    // predominantly from one side is less threatening on this map than one
    // who dominates both halves.
    // =========================================================================

    /**
     * Simulates the CS2 BO3 veto.
     *
     * @return int[3]: [team1Pick, team2Pick, decider] as map-pool indices.
     */
    @Contract("_, _ -> new")
    private static int @NotNull [] simulateVeto(@NotNull Team t1, @NotNull Team t2) {
        boolean[] avail = new boolean[NUM_MAPS];
        Arrays.fill(avail, true);

        // Steps 1–2: opponent-aware first bans
        applyBan(avail, firstBanWeights(t1, t2, avail));
        applyBan(avail, firstBanWeights(t2, t1, avail));

        // Steps 3–4: CT/T-matchup-adjusted picks → Maps 1 and 2
        int map1 = applyPick(avail, pickWeights(t1, t2, avail));
        int map2 = applyPick(avail, pickWeights(t2, t1, avail));

        // Steps 5–6: opponent-targeting second bans
        applyBan(avail, secondBanWeights(t1, t2, avail));
        applyBan(avail, secondBanWeights(t2, t1, avail));

        // Step 7: last surviving map is the decider
        int map3 = -1;
        for (int i = 0; i < NUM_MAPS; i++) {
            if (avail[i]) {
                map3 = i;
                break;
            }
        }
        return new int[]{map1, map2, map3};
    }

    /**
     * First-ban weights: sq(0.55 × selfAvoid + 0.45 × oppStrength).
     *
     * <p>The highest-priority first ban is a map the team wants to avoid AND
     * where the opponent is strong. {@code effectiveBanPct} handles maps with
     * zero play time due to systematic avoidance. {@code effectiveMapStrength}
     * accounts for CT/T side balance in the opponent's threat level — an
     * opponent dominant on both sides is more dangerous than one whose WR
     * comes entirely from their preferred half.
     */
    private static double @NotNull [] firstBanWeights(@NotNull Team self, @NotNull Team opp, boolean[] avail) {
        double[] w = new double[NUM_MAPS];

        for (int i = 0; i < NUM_MAPS; i++) {
            if (avail[i]) {
                double selfAvoid = effectiveBanPct(self, i);
                double oppStrength = effectiveMapStrength(opp, i);
                double combined = 0.55 * selfAvoid + 0.45 * oppStrength;
                w[i] = combined * combined;
            }
        }
        return w;
    }

    /**
     * Pick weights: sq(effectivePct(pick%, n)) × matchupMultiplier.
     *
     * <p>Teams pick comfort maps adjusted by a matchup-specific multiplier.
     * When both teams have CT/T side data on a map, the multiplier uses a
     * blend of overall WR differential and CT/T-balanced round win rate
     * differential — a map where the picking team dominates both sides is
     * more valuable than one where their advantage depends on winning the
     * knife round. When side data is unavailable for either team, the
     * multiplier falls back to the WR differential alone.
     * Multiplier is clamped to [0.5, 1.75].
     */
    private static double @NotNull [] pickWeights(@NotNull Team self, @NotNull Team opp, boolean[] avail) {
        double[] w = new double[NUM_MAPS];

        for (int i = 0; i < NUM_MAPS; i++) {
            if (avail[i]) {
                int selfN = getMapN(self, i);
                int oppN = getMapN(opp, i);
                double selfWR = effectiveMapWR(getWinRate(self, i), selfN);
                double oppWR = effectiveMapWR(getWinRate(opp, i), oppN);
                double relAdv;

                if (hasSideData(self, i) && hasSideData(opp, i)) {
                    // Both teams have CT/T data — blend WR advantage with
                    // side-balanced round win rate advantage.
                    double selfAvgRound = (getWinRateCT(self, i) + getWinRateT(self, i)) / 2.0;
                    double oppAvgRound = (getWinRateCT(opp, i) + getWinRateT(opp, i)) / 2.0;
                    relAdv = 0.60 * (selfWR - oppWR) / 100.0 + 0.40 * (selfAvgRound - oppAvgRound) / 100.0;
                } else {
                    relAdv = (selfWR - oppWR) / 100.0;
                }

                double base = effectivePct(getPickPct(self, i), selfN);
                double mult = StrictMath.max(0.5, StrictMath.min(1.75, 1.0 + relAdv * 0.75));
                w[i] = base * base * mult;
            }
        }
        return w;
    }

    /**
     * Second-ban weights: 20% own avoidance + 80% opponent map strength.
     *
     * <p>By this stage the team has already removed its worst map. The
     * dominant objective is denying the opponent their best remaining option.
     * {@code effectiveMapStrength} incorporates CT/T balance so that a map
     * where the opponent only thrives on one side receives proportionally
     * less denial weight.
     */
    private static double @NotNull [] secondBanWeights(@NotNull Team self, @NotNull Team opp, boolean[] avail) {
        double[] w = new double[NUM_MAPS];

        for (int i = 0; i < NUM_MAPS; i++) {
            if (avail[i]) {
                double selfAvoid = effectiveBanPct(self, i);
                double oppStrength = effectiveMapStrength(opp, i);
                w[i] = 0.20 * (selfAvoid * selfAvoid) + 0.80 * (oppStrength * oppStrength);
            }
        }
        return w;
    }

    /**
     * Removes the weighted-randomly selected map from the available pool (ban step).
     */
    private static void applyBan(boolean[] avail, double[] weights) {
        int idx = weightedRandom(avail, weights);

        if (idx >= 0) {
            avail[idx] = false;
        }
    }

    /**
     * Removes the weighted-randomly selected map from the available pool (pick step)
     * and returns its index.
     */
    private static int applyPick(boolean[] avail, double[] weights) {
        int idx = weightedRandom(avail, weights);

        if (idx >= 0) {
            avail[idx] = false;
        }
        return idx;
    }

    /**
     * Weighted-random selection across available maps using ThreadLocalRandom.
     * Falls back to uniform selection when all weights are zero, ensuring the
     * veto always completes regardless of data availability.
     *
     * @return Map index, or -1 if no maps remain.
     */
    private static int weightedRandom(boolean[] avail, double[] weights) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double total = 0.0;

        for (int i = 0; i < NUM_MAPS; i++) {
            if (avail[i]) {
                total += weights[i];
            }
        }

        if (total <= 0.0) {
            int count = 0;

            for (boolean b : avail) {
                if (b) {
                    count++;
                }
            }

            if (count == 0) {
                return -1;
            }

            int target = rng.nextInt(count);
            int seen = 0;

            for (int i = 0; i < NUM_MAPS; i++) {
                if (!avail[i]) {
                    continue;
                }

                if (seen == target) {
                    return i;
                }

                seen++;
            }
            return -1;
        }

        double r = rng.nextDouble() * total;
        double cum = 0.0;

        for (int i = 0; i < NUM_MAPS; i++) {
            if (!avail[i]) {
                continue;
            }

            cum += weights[i];

            if (r <= cum) {
                return i;
            }
        }

        // Floating-point rounding guard
        for (int i = NUM_MAPS - 1; i >= 0; i--) {
            if (avail[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Calculates team 1's win probability on a specific map.
     *
     * <p>Three signals are blended using a sample-size-adaptive weight:
     * <ol>
     *   <li><b>Map WR signal</b>: ratio of each team's overall win rate on
     *       this map. A 10% floor prevents 0% WR from collapsing the
     *       probability entirely.</li>
     *   <li><b>CT/T side signal</b> (incorporated when both teams have
     *       ≥ 3 appearances and non-zero side rates): ratio of each team's
     *       expected round win rate, computed as the average of their CT and T
     *       round win percentages. This correctly penalises teams whose map
     *       strength is concentrated on one side — a team that wins 68% of CT
     *       rounds but only 37% of T rounds has a lower expected round win rate
     *       than one that wins 56% on both sides, even if their overall WR
     *       looks comparable. The two map signals are blended 55% WR / 45%
     *       CT/T when side data is available, falling back to WR-only otherwise.</li>
     *   <li><b>Overall quality signal</b> (1 − adaptive weight): the team
     *       score win probability from {@link #calculateWinProbability},
     *       anchoring the result against small-sample map noise.</li>
     * </ol>
     *
     * <p>Blend weight by sample size (minimum of both teams' per-map counts):
     * n=0→0%, n=1–2→15%, n=3–4→30%, n=5–6→45%, n=7–8→55%, n=9–12→65%, n≥13→70%.
     *
     * @param t1     Team 1 (the team whose probability is returned).
     * @param t2     Team 2.
     * @param mapIdx Map-pool index from the constants at the top of this class.
     * @return Win probability for team 1, clamped to [0.12, 0.88].
     */
    private static double calculateMapWinProbability(@NotNull Team t1, @NotNull Team t2, int mapIdx) {
        double overallProb = calculateWinProbability(t1, t2);
        int t1n = getMapN(t1, mapIdx);
        int t2n = getMapN(t2, mapIdx);
        final double FLOOR = 0.10;

        // Map WR signal
        double t1wr = getWinRate(t1, mapIdx) / 100.0;
        double t2wr = getWinRate(t2, mapIdx) / 100.0;
        double e1wr = StrictMath.max(FLOOR, t1wr);
        double e2wr = StrictMath.max(FLOOR, t2wr);
        double wrSignal = e1wr / (e1wr + e2wr);

        // CT/T side signal (blended in when reliable side data exists)
        double combinedMapSignal;
        if (hasSideData(t1, mapIdx) && hasSideData(t2, mapIdx)) {
            double t1AvgRound = (getWinRateCT(t1, mapIdx) + getWinRateT(t1, mapIdx)) / 200.0;
            double t2AvgRound = (getWinRateCT(t2, mapIdx) + getWinRateT(t2, mapIdx)) / 200.0;
            double e1s = StrictMath.max(FLOOR, t1AvgRound);
            double e2s = StrictMath.max(FLOOR, t2AvgRound);
            double sideSignal = e1s / (e1s + e2s);

            // 55% WR-based, 45% CT/T-based
            combinedMapSignal = 0.55 * wrSignal + 0.45 * sideSignal;
        } else {
            combinedMapSignal = wrSignal;
        }

        // Adaptive blend with overall quality signal
        double w = StrictMath.min(mapBlendWeight(t1n), mapBlendWeight(t2n));
        double blended = w * combinedMapSignal + (1.0 - w) * overallProb;
        return StrictMath.min(0.88, StrictMath.max(0.12, blended));
    }

    /**
     * Calculates team 1's win probability from the composite team score.
     *
     * <p>The score ratio passes through a logit → sigmoid pipeline. The sigmoid
     * scale is reduced from 2.5 toward a floor of 1.8 for rosters with a large
     * gap between mean and minimum HLTV rating (high-spread teams are more
     * volatile — more likely to either over- or under-perform their average).
     *
     * @param t1 The first team.
     * @param t2 The second team.
     * @return Win probability for team 1 in [0.12, 0.88].
     */
    private static double calculateWinProbability(@NotNull Team t1, @NotNull Team t2) {
        double s1 = StrictMath.max(1.0e-9, adjustedTeamScore(t1));
        double s2 = StrictMath.max(1.0e-9, adjustedTeamScore(t2));
        double signal = s1 / (s1 + s2);

        double drag = StrictMath.max(teamDrag(t1), teamDrag(t2));
        double sigScale = StrictMath.max(1.8, 2.5 - drag * 1.5);

        double logit = StrictMath.log(signal / (1.0 - signal));
        double raw = 1.0 / (1.0 + StrictMath.exp(-sigScale * logit));
        return StrictMath.min(0.88, StrictMath.max(0.12, raw));
    }

    /**
     * Computes the composite performance score for a single team from all
     * six available metrics, each filtered to top-50 matches in the last
     * three months.
     *
     * <p><b>Weights and normalisations:</b>
     * <ul>
     *   <li>Bayesian WR (40%): (mapsWon + 1) / (mapsPlayed + 2). Already in [0, 1].</li>
     *   <li>Mean round swing (25%): (mean + 2.0) / 3.0, clamped [0, 1].
     *       Covers the observed team-mean range of approximately [−2.0, +1.0].</li>
     *   <li>Mean trading score (15%): direct mean, already in [0, 1].</li>
     *   <li>Mean opening score (10%): direct mean, already in [0, 1].</li>
     *   <li>Mean firepower (6%): (mean − 0.15) / 0.60, clamped [0, 1].
     *       Covers the observed team-mean range of approximately [0.15, 0.75].</li>
     *   <li>Mean HLTV rating (4%): (mean − 0.85) / 0.40, clamped [0, 1].
     *       Covers the observed team-mean range of approximately [0.85, 1.25].</li>
     * </ul>
     *
     * @param t The team.
     * @return Composite score, nominally in [0, 1].
     */

    /**
     * The floor score applied to teams with very few maps played vs top-50.
     *
     * <p>A team with a tiny sample (3 maps, 1 win) has misleadingly uncertain
     * per-player metrics and a Laplace-smoothed win rate that converges toward
     * the 50% neutral prior rather than reflecting their likely true quality.
     * The floor (0.33) represents a below-average team — one we expect to lose
     * more than they win — which is an appropriate conservative prior for any
     * team that has barely played at the top-50 level.
     *
     * <p>At 20 total maps played, confidence reaches 1.0 and the adjustment
     * has no effect. Below that, the raw score is linearly blended toward
     * this floor, with 3-map teams at 15% confidence (85% floor weight) and
     * 10-map teams at 50% confidence.
     */
    private static final double LOW_SAMPLE_FLOOR_SCORE = 0.33;

    /**
     * Applies a sample-size confidence penalty to the raw team score.
     *
     * <p>Blends the raw {@link #teamScore} with {@link #LOW_SAMPLE_FLOOR_SCORE}
     * using confidence = min(1.0, mapsPlayed / 20.0). This corrects two
     * structural problems that arise for teams with very few recorded maps
     * vs top-50 opponents:
     *
     * <ol>
     *   <li>The Laplace-smoothed win rate pulls toward the neutral 50% prior
     *       rather than a pessimistic one — a team with 1 win in 3 maps gets
     *       a Laplace BWR of 40%, appearing similar to a team with 16 wins in
     *       48 maps (34%). The floor pulls the 3-map team well below the
     *       48-map team.</li>
     *   <li>Per-player metrics (swing, trading, opening, firepower, HLTV rating)
     *       computed over 3 maps may reflect two or three extraordinary individual
     *       performances rather than stable team quality. Blending the overall
     *       score toward the floor indirectly dampens the influence of these
     *       potentially coincidental high values.</li>
     * </ol>
     *
     * <p>Teams with ≥ 20 maps are unaffected (confidence = 1.0). The floor
     * does not make low-sample teams play at exactly the floor score — it
     * reduces their expected quality to be appropriately uncertain and
     * conservative, which is the correct representation of unknown quantity.
     *
     * @param t The team.
     * @return Confidence-adjusted composite score.
     */
    public static double adjustedTeamScore(@NotNull Team t) {
        double raw = teamScore(t);
        double sampleConf = StrictMath.min(1.0, t.getMapsPlayed() / 20.0);
        return sampleConf * raw + (1.0 - sampleConf) * LOW_SAMPLE_FLOOR_SCORE;
    }

    /**
     * OPN normalisation range.
     *
     * <p>Opponent Network scores in a Major bracket fall roughly between 40
     * (pure regional teams that only beat local opponents) and 180 (top
     * global teams that beat diverse high-quality competition). The range
     * [40, 180] covers this with a small buffer on each end.
     *
     * <p>Specifically, this bracket spans OPN 50 (Sharks) to 172 (B8).
     * Sharks' minimum score of 0.07 reflects that nearly all of their wins
     * are against SA regional opponents whose own network breadth is low.
     * B8's score of 0.94 reflects wins against a wide variety of high-quality
     * EU opponents across multiple event circuits.
     */
    private static final double OPN_NORM_MIN = 40.0;
    private static final double OPN_NORM_RANGE = 140.0;

    private static double teamScore(@NotNull Team t) {
        // 1. Bayesian map win rate (37%)
        double bwr = (t.getMapsWon() + 1.0) / (t.getMapsPlayed() + 2.0);

        // 2. Mean round swing, normalised from [-2.0, +1.0] to [0, 1] (22%)
        double rsMean = listMean(t.getRoundSwing());
        double rsNorm = StrictMath.min(1.0, StrictMath.max(0.0, (rsMean + 2.0) / 3.0));

        // 3. Mean trading score (already 0–1) (12%)
        double trMean = listMean(t.getTrading());

        // 4. Opponent Network, normalised to [0, 1] (10%)
        //    OPN measures the breadth and quality of opponents beaten: wins are
        //    weighted by how many different teams the opponent themselves has
        //    beaten, recency, and event prize pool. This is the primary
        //    cross-regional quality corrective — a team that only beats SA or
        //    AS regional opponents produces a low OPN regardless of their raw
        //    win rate, while a team beating diverse global top-50 opponents
        //    scores high. This directly addresses SA/AS regional inflation:
        //    Sharks (OPN 50) is penalised relative to HEROIC (OPN 97) even if
        //    their raw win rates and per-player metrics appear similar.
        double opnNorm = StrictMath.min(1.0, StrictMath.max(0.0,
                (t.getOpponentNetwork() - OPN_NORM_MIN) / OPN_NORM_RANGE));

        // 5. Mean opening score (already 0–1) (9%)
        double opMean = listMean(t.getOpening());

        // 6. Mean firepower, normalised from [0.15, 0.75] to [0, 1] (5%)
        double fpMean = listMean(t.getFirepower());
        double fpNorm = StrictMath.min(1.0, StrictMath.max(0.0, (fpMean - 0.15) / 0.60));

        // 7. Mean HLTV rating, normalised from [0.85, 1.25] to [0, 1] (5%)
        double hlMean = listMean(t.getHltvRating());
        double hlNorm = StrictMath.min(1.0, StrictMath.max(0.0, (hlMean - 0.85) / 0.40));

        return 0.37 * bwr
                + 0.22 * rsNorm
                + 0.12 * trMean
                + 0.10 * opnNorm
                + 0.09 * opMean
                + 0.05 * fpNorm
                + 0.05 * hlNorm;
    }

    /**
     * Returns true if this team has at least 10 total maps played vs top-50,
     * the minimum required for reliable veto simulation and per-map probabilities.
     */
    @Contract(pure = true)
    private static boolean hasMapData(@NotNull Team t) {
        return t.getMapsPlayed() >= 10;
    }

    /**
     * Returns true if reliable CT/T side data exists for this team on the
     * given map. Requires at least 3 map appearances and non-zero values for
     * both CT and T win rates (zero indicates missing data, not a 0% win rate
     * on that side, which would be captured by the very small sample size).
     */
    private static boolean hasSideData(@NotNull Team t, int mapIdx) {
        return getMapN(t, mapIdx) >= 3
                && getWinRateCT(t, mapIdx) > 0.0
                && getWinRateT(t, mapIdx) > 0.0;
    }

    /**
     * Adaptive blend weight for a per-map sample size.
     * n=0: 0%, n=1–2: 15%, n=3–4: 30%, n=5–6: 45%, n=7–8: 55%, n=9–12: 65%, n≥13: 70%.
     */
    @Contract(pure = true)
    private static double mapBlendWeight(int n) {
        if (n == 0) {
            return 0.00;
        }
        if (n <= 2) {
            return 0.15;
        }
        if (n <= 4) {
            return 0.30;
        }
        if (n <= 6) {
            return 0.45;
        }
        if (n <= 8) {
            return 0.55;
        }
        if (n <= 12) {
            return 0.65;
        }
        return 0.70;
    }

    /**
     * Confidence-blended pick or ban percentage.
     * Blends the observed percentage with a uniform prior (100/7 ≈ 14.3%)
     * using confidence = min(1.0, mapN / 7.0). Prevents small-sample
     * coincidences from dominating after squaring.
     */
    @Contract(pure = true)
    private static double effectivePct(double observedPct, int mapN) {
        double confidence = StrictMath.min(1.0, mapN / 7.0);
        double uniformPrior = 100.0 / NUM_MAPS;
        return confidence * observedPct + (1.0 - confidence) * uniformPrior;
    }

    /**
     * Confidence-blended ban percentage with special handling for systematically
     * avoided maps. When a map has zero play time but a high observed ban rate
     * (≥ 50%), the absence of appearances is itself evidence of consistent
     * avoidance — the team always bans it before it can be played. Confidence
     * is derived from total maps played (25 maps → full confidence) rather than
     * the per-map count.
     */
    private static double effectiveBanPct(@NotNull Team t, int mapIdx) {
        double observedBan = getBanPct(t, mapIdx);
        int mapN = getMapN(t, mapIdx);

        if (mapN == 0 && observedBan >= 50.0) {
            double confidence = StrictMath.min(1.0, t.getMapsPlayed() / 25.0);
            double uniformPrior = 100.0 / NUM_MAPS;
            return confidence * observedBan + (1.0 - confidence) * uniformPrior;
        }
        return effectivePct(observedBan, mapN);
    }

    /**
     * Confidence-blended win rate for veto threat assessment.
     * Blends toward a neutral 50% for low-sample maps.
     *
     * @param observedWR Observed win rate (0–100).
     * @param mapN       Maps played on this specific map.
     * @return Confidence-blended effective win rate.
     */
    @Contract(pure = true)
    private static double effectiveMapWR(double observedWR, int mapN) {
        double confidence = StrictMath.min(1.0, mapN / 7.0);
        return confidence * observedWR + (1.0 - confidence) * 50.0;
    }

    /**
     * CT/T-balanced opponent map strength for ban weight calculations.
     *
     * <p>When the opponent has reliable CT/T data on this map, blends the
     * overall WR (60%) with their CT/T-balanced average round win rate (40%).
     * A team whose WR is concentrated on one side — e.g. 80% CT but 35% T —
     * is less threatening to ban than one who dominates both sides at 60%+,
     * because the former can be countered by winning the knife round. When
     * side data is absent, falls back to {@link #effectiveMapWR}.
     *
     * @param opp    The opposing team.
     * @param mapIdx The map index.
     * @return Confidence-blended effective map threat level (0–100 scale).
     */
    private static double effectiveMapStrength(@NotNull Team opp, int mapIdx) {
        int oppN = getMapN(opp, mapIdx);

        if (hasSideData(opp, mapIdx)) {
            double oppWR = getWinRate(opp, mapIdx);
            double oppAvgRound = (getWinRateCT(opp, mapIdx) + getWinRateT(opp, mapIdx)) / 2.0;
            double blended = 0.60 * oppWR + 0.40 * oppAvgRound;
            double confidence = StrictMath.min(1.0, oppN / 7.0);
            return confidence * blended + (1.0 - confidence) * 50.0;
        }
        return effectiveMapWR(getWinRate(opp, mapIdx), oppN);
    }

    /**
     * Computes the HLTV rating drag for a team: mean rating minus the weakest
     * player's rating. A larger gap indicates a more imbalanced roster with
     * higher match-outcome variance. Returns 0 when data is unavailable.
     */
    private static double teamDrag(@NotNull Team t) {
        List<Double> ratings = t.getHltvRating();

        if (ratings == null || ratings.size() < 2) {
            return 0.0;
        }

        double mean = listMean(ratings);
        double min = ratings.stream().mapToDouble(Double::doubleValue).min().orElse(1.0);
        return StrictMath.max(0.0, mean - min);
    }

    /**
     * Computes the arithmetic mean of a list of doubles.
     * Returns 0.0 for null or empty lists.
     */
    private static double listMean(Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;

        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private static int getMapN(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMirageMapsPlayed();
            case INFERNO -> t.getInfernoMapsPlayed();
            case NUKE -> t.getNukeMapsPlayed();
            case ANCIENT -> t.getAncientMapsPlayed();
            case OVERPASS -> t.getOverpassMapsPlayed();
            case ANUBIS -> t.getAnubisMapsPlayed();
            case DUST2 -> t.getDustIIMapsPlayed();
            default -> 0;
        };
    }

    private static double getWinRate(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMirageWinRate();
            case INFERNO -> t.getInfernoWinRate();
            case NUKE -> t.getNukeWinRate();
            case ANCIENT -> t.getAncientWinRate();
            case OVERPASS -> t.getOverpassWinRate();
            case ANUBIS -> t.getAnubisWinRate();
            case DUST2 -> t.getDustIIWinRate();
            default -> 0.0;
        };
    }

    private static double getWinRateCT(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMirageWinRateCT();
            case INFERNO -> t.getInfernoWinRateCT();
            case NUKE -> t.getNukeWinRateCT();
            case ANCIENT -> t.getAncientWinRateCT();
            case OVERPASS -> t.getOverpassWinRateCT();
            case ANUBIS -> t.getAnubisWinRateCT();
            case DUST2 -> t.getDustIIWinRateCT();
            default -> 0.0;
        };
    }

    private static double getWinRateT(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMirageWinRateT();
            case INFERNO -> t.getInfernoWinRateT();
            case NUKE -> t.getNukeWinRateT();
            case ANCIENT -> t.getAncientWinRateT();
            case OVERPASS -> t.getOverpassWinRateT();
            case ANUBIS -> t.getAnubisWinRateT();
            case DUST2 -> t.getDustIIWinRateT();
            default -> 0.0;
        };
    }

    private static double getPickPct(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMiragePickPercent();
            case INFERNO -> t.getInfernoPickPercent();
            case NUKE -> t.getNukePickPercent();
            case ANCIENT -> t.getAncientPickPercent();
            case OVERPASS -> t.getOverpassPickPercent();
            case ANUBIS -> t.getAnubisPickPercent();
            case DUST2 -> t.getDustIIPickPercent();
            default -> 0.0;
        };
    }

    private static double getBanPct(@NotNull Team t, int map) {
        return switch (map) {
            case MIRAGE -> t.getMirageBanPercent();
            case INFERNO -> t.getInfernoBanPercent();
            case NUKE -> t.getNukeBanPercent();
            case ANCIENT -> t.getAncientBanPercent();
            case OVERPASS -> t.getOverpassBanPercent();
            case ANUBIS -> t.getAnubisBanPercent();
            case DUST2 -> t.getDustIIBanPercent();
            default -> 0.0;
        };
    }

    /**
     * Displays the win probability for a head-to-head matchup.
     *
     * @param t1 The first team.
     * @param t2 The second team.
     */
    public static void displayWinnerFromProbability(@NotNull Team t1,
                                                    @NotNull Team t2) {
        double p = calculateWinProbability(t1, t2);

        if (p >= 0.5) {
            System.out.println(t1.getName() + " has a " + p * 100
                    + "% chance of winning against " + t2.getName());
        } else {
            System.out.println(t2.getName() + " has a " + (1 - p) * 100
                    + "% chance of winning against " + t1.getName());
        }
    }
}
