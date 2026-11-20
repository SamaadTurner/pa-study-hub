package com.pastudyhub.flashcard.engine;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fill-in-the-blank ("cloze") text in Anki's syntax: {{c1::answer}} or, with a
 * hint, {{c1::answer::hint}}. The number says which card asks for that blank:
 * every number becomes its own card, and the same number may appear several
 * times when the blanks belong together.
 *
 * <p>Pure logic, no Spring. The frontend has the same rules in utils/cloze.ts.
 */
public final class ClozeText {

    /** Anki allows more, but a card with 20 separate blanks is not a flashcard any more. */
    public static final int MAX_NUMBER = 20;

    // {{c<number>::<answer>}} or {{c<number>::<answer>::<hint>}}; the answer can't contain "}}"
    private static final Pattern BLANK = Pattern.compile("\\{\\{c(\\d{1,2})::(.+?)(?:::(.*?))?}}", Pattern.DOTALL);

    private ClozeText() {
    }

    /** The blank numbers used in the text, smallest first. Empty when there are none. */
    public static SortedSet<Integer> numbers(String text) {
        SortedSet<Integer> numbers = new TreeSet<>();
        if (text == null) {
            return numbers;
        }
        Matcher matcher = BLANK.matcher(text);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group(1)));
        }
        return numbers;
    }

    /**
     * What is wrong with the text as a cloze card, or null when it is fine.
     * The message is meant for the person writing it.
     */
    public static String problem(String text) {
        SortedSet<Integer> numbers = numbers(text);
        if (numbers.isEmpty()) {
            return "Mark at least one blank like this: {{c1::the answer}}";
        }
        if (numbers.first() < 1 || numbers.last() > MAX_NUMBER) {
            return "Blank numbers go from c1 to c" + MAX_NUMBER;
        }
        return null;
    }
}
