package com.crm.service.analytics;

import com.crm.domain.analytics.ChurnRisk;
import com.crm.domain.analytics.RfmSegment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LtvCalculationServiceTest {

    @Test
    void champion_maps_to_low_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.CHAMPION))
                .isEqualTo(ChurnRisk.LOW);
    }

    @Test
    void loyal_maps_to_low_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.LOYAL))
                .isEqualTo(ChurnRisk.LOW);
    }

    @Test
    void at_risk_maps_to_medium_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.AT_RISK))
                .isEqualTo(ChurnRisk.MEDIUM);
    }

    @Test
    void hibernating_maps_to_medium_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.HIBERNATING))
                .isEqualTo(ChurnRisk.MEDIUM);
    }

    @Test
    void cant_lose_maps_to_high_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.CANT_LOSE))
                .isEqualTo(ChurnRisk.HIGH);
    }

    @Test
    void lost_maps_to_high_churn_risk() {
        assertThat(LtvCalculationService.deriveChurnRisk(RfmSegment.LOST))
                .isEqualTo(ChurnRisk.HIGH);
    }
}
