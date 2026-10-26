package io.github.donnyyu.cleandash;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeywordsTest {

    @Test
    public void matchesWholeWords() {
        assertTrue(Keywords.matches("FM Radio", new String[]{"radio"}));
        assertTrue(Keywords.matches("ZLink", new String[]{"carplay", "zlink"}));
        assertTrue(Keywords.matches("CarPlay-Wireless", new String[]{"carplay"}));
        assertTrue(Keywords.matches("EQ", new String[]{"eq"}));
    }

    @Test
    public void ignoresWordsInsideOtherWords() {
        assertFalse(Keywords.matches("Frequency", new String[]{"eq"}));
        assertFalse(Keywords.matches("Radiology", new String[]{"radio"}));
        assertFalse(Keywords.matches("Navigator Pro", new String[]{"navi"}));
    }

    @Test
    public void matchesPhrases() {
        assertTrue(Keywords.matches("Android Auto", new String[]{"android auto"}));
        assertFalse(Keywords.matches("Android Settings", new String[]{"android auto"}));
    }

    @Test
    public void emptyInputsNeverMatch() {
        assertFalse(Keywords.matches(null, new String[]{"radio"}));
        assertFalse(Keywords.matches("Radio", new String[]{}));
    }
}
