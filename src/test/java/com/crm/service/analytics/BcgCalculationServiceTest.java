package com.crm.service.analytics;

import com.crm.domain.analytics.BcgQuadrant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BcgCalculationServiceTest {

    @Test
    void star_when_high_share_and_high_growth() {
        assertThat(BcgCalculationService.assignQuadrant(60, 20, 50, 10))
                .isEqualTo(BcgQuadrant.STAR);
    }

    @Test
    void cash_cow_when_high_share_and_low_growth() {
        assertThat(BcgCalculationService.assignQuadrant(60, 5, 50, 10))
                .isEqualTo(BcgQuadrant.CASH_COW);
    }

    @Test
    void question_mark_when_low_share_and_high_growth() {
        assertThat(BcgCalculationService.assignQuadrant(20, 30, 50, 10))
                .isEqualTo(BcgQuadrant.QUESTION_MARK);
    }

    @Test
    void dog_when_low_share_and_low_growth() {
        assertThat(BcgCalculationService.assignQuadrant(10, 2, 50, 10))
                .isEqualTo(BcgQuadrant.DOG);
    }

    @Test
    void median_of_odd_list() {
        assertThat(BcgCalculationService.median(List.of(1.0, 3.0, 5.0))).isEqualTo(3.0);
    }

    @Test
    void median_of_even_list() {
        assertThat(BcgCalculationService.median(List.of(1.0, 2.0, 3.0, 4.0))).isEqualTo(2.5);
    }

    @Test
    void median_of_single_element() {
        assertThat(BcgCalculationService.median(List.of(7.0))).isEqualTo(7.0);
    }
}
