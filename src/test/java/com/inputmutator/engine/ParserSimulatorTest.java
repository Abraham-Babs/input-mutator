package com.inputmutator.engine;

import com.inputmutator.engine.pipeline.ParserSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserSimulatorTest {

    private ParserSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new ParserSimulator();
    }

    @Test
    void testSimulateUrlDecoded() {
        ParserSimulator.SimulationResult result = simulator.simulate("%61dmin", "admin");
        assertEquals("admin", result.urlDecoded());
        assertTrue(result.matchesOriginal());
    }

    @Test
    void testSimulateNfkcNormalized() {
        // Ligature ff (\uFB00) normalizes to ff under NFKC
        ParserSimulator.SimulationResult result = simulator.simulate("o\uFB00ice", "office");
        assertEquals("office", result.nfkcNormalized());
        assertTrue(result.matchesOriginal());
    }

    @Test
    void testSimulateHtmlUnescape() {
        ParserSimulator.SimulationResult result = simulator.simulate("&quot;test&quot;", "\"test\"");
        assertEquals("\"test\"", result.htmlUnescaped());
        assertTrue(result.matchesOriginal());
    }

    @Test
    void testSimulateNoMatch() {
        ParserSimulator.SimulationResult result = simulator.simulate("distinctValue", "admin");
        assertFalse(result.matchesOriginal());
    }
}
