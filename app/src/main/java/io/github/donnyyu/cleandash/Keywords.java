package io.github.donnyyu.cleandash;

import java.util.Locale;

/** Matches an app's name against a tile's keywords. Plain Java, so it is unit tested on the JVM. */
final class Keywords {

    private Keywords() {}

    /**
     * True when the label holds one of the keywords as a whole word ("FM Radio" matches "radio", "Frequency"
     * does not match "eq"). A keyword with a space ("android auto") matches as a phrase.
     */
    static boolean matches(CharSequence label, String[] keywords) {
        if (label == null || keywords.length == 0) return false;
        String words = " " + label.toString().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim() + " ";
        for (String keyword : keywords) {
            if (words.contains(" " + keyword + " ")) return true;
        }
        return false;
    }
}
