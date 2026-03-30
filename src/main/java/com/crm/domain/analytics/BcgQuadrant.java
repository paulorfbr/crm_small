package com.crm.domain.analytics;

/**
 * BCG Growth-Share Matrix quadrants adapted for B2B customer portfolio analysis.
 *
 * <ul>
 *   <li>STAR          — high revenue share + high growth. Core accounts to invest in.</li>
 *   <li>CASH_COW      — high revenue share + low growth. Stable, profitable; protect and maximize.</li>
 *   <li>QUESTION_MARK — low revenue share + high growth. Potential upside; needs nurturing.</li>
 *   <li>DOG           — low revenue share + low growth. Re-evaluate or exit.</li>
 * </ul>
 *
 * Revenue share threshold: portfolio median (relative, not absolute).
 * Growth threshold: portfolio median YoY growth rate.
 */
public enum BcgQuadrant {
    STAR,
    CASH_COW,
    QUESTION_MARK,
    DOG
}
