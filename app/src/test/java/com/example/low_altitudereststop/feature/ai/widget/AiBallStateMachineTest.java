package com.example.low_altitudereststop.feature.ai.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiBallStateMachineTest {

    @Test
    public void idleMode_isCollapsedByDefault() {
        AiBallStateMachine machine = new AiBallStateMachine();
        assertEquals(AiBallStateMachine.MODE_IDLE, machine.currentMode());
        assertFalse(machine.isExpanded());
    }

    @Test
    public void nonIdleModes_areExpanded() {
        AiBallStateMachine machine = new AiBallStateMachine();
        machine.update(AiBallStateMachine.MODE_ACTIVE);
        machine.setExpanded(true);
        assertTrue(machine.isExpanded());
        machine.update(AiBallStateMachine.MODE_THINKING);
        assertEquals(AiBallStateMachine.MODE_THINKING, machine.currentMode());
        assertTrue(machine.isExpanded());
    }
}
