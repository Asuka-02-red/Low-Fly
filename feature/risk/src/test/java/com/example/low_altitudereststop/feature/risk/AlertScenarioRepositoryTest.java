package com.example.low_altitudereststop.feature.risk;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AlertScenarioRepositoryTest {

    @Test
    public void shouldBuildConsistentAlertScenarios() {
        List<AlertScenarioRepository.AlertScenario> alerts = AlertScenarioRepository.buildAlerts("PILOT");

        Assert.assertFalse(alerts.isEmpty());
        AlertScenarioRepository.AlertScenario first = alerts.get(0);
        Assert.assertNotNull(first.alert.id);
        Assert.assertFalse(first.riskDescription.isEmpty());
        Assert.assertFalse(first.suggestion.isEmpty());
    }

    @Test
    public void shouldFindAlertDetailFromSameMockSource() {
        List<AlertScenarioRepository.AlertScenario> alerts = AlertScenarioRepository.buildAlerts("ENTERPRISE");
        AlertScenarioRepository.AlertScenario match = AlertScenarioRepository.findById("ENTERPRISE", alerts.get(0).alert.id);

        Assert.assertNotNull(match);
        Assert.assertEquals(alerts.get(0).alert.id, match.alert.id);
        Assert.assertEquals(alerts.get(0).alert.content, match.alert.content);
    }
}
