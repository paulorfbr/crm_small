package com.crm.service.analytics;

import com.crm.domain.analytics.RfmSegment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RfmCalculationServiceTest {

    @Test
    void champion_when_all_scores_are_high() {
        assertThat(RfmCalculationService.assignSegment((short)5, (short)5, (short)5))
                .isEqualTo(RfmSegment.CHAMPION);
        assertThat(RfmCalculationService.assignSegment((short)4, (short)4, (short)4))
                .isEqualTo(RfmSegment.CHAMPION);
    }

    @Test
    void cant_lose_when_low_recency_high_freq_and_monetary() {
        assertThat(RfmCalculationService.assignSegment((short)1, (short)5, (short)5))
                .isEqualTo(RfmSegment.CANT_LOSE);
        assertThat(RfmCalculationService.assignSegment((short)2, (short)4, (short)4))
                .isEqualTo(RfmSegment.CANT_LOSE);
    }

    @Test
    void at_risk_when_low_recency_medium_freq_and_monetary() {
        assertThat(RfmCalculationService.assignSegment((short)2, (short)3, (short)3))
                .isEqualTo(RfmSegment.AT_RISK);
        assertThat(RfmCalculationService.assignSegment((short)1, (short)4, (short)3))
                .isEqualTo(RfmSegment.AT_RISK);
    }

    @Test
    void new_customer_when_recency_5_frequency_1() {
        assertThat(RfmCalculationService.assignSegment((short)5, (short)1, (short)3))
                .isEqualTo(RfmSegment.NEW);
    }

    @Test
    void loyal_when_decent_recency_high_frequency() {
        assertThat(RfmCalculationService.assignSegment((short)3, (short)4, (short)2))
                .isEqualTo(RfmSegment.LOYAL);
    }

    @Test
    void potential_loyalist_when_high_recency_low_frequency() {
        assertThat(RfmCalculationService.assignSegment((short)4, (short)2, (short)3))
                .isEqualTo(RfmSegment.POTENTIAL_LOYALIST);
    }

    @Test
    void hibernating_when_low_recency_and_low_frequency() {
        assertThat(RfmCalculationService.assignSegment((short)2, (short)2, (short)2))
                .isEqualTo(RfmSegment.HIBERNATING);
    }
}
