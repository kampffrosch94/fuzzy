package com.redfin.fuzzy.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.redfin.fuzzy.Any;
import com.redfin.fuzzy.Case;
import com.redfin.fuzzy.FuzzyUtil;
import com.redfin.fuzzy.Literal;
import com.redfin.fuzzy.Subcase;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class StringCaseTest {

	private Random random;

	@Before
	public void before() {
		random = new Random(12345); // keep tests consistent
	}

	@Test
	public void testDefaultConfig() {
		Case<String> subject = Any.string();
		Set<Subcase<String>> subcases = subject.getSubcases();

		boolean foundStandard, foundWhitespace, foundUnicode, foundEmoji, foundInjection, foundEmpty;
		foundStandard = foundWhitespace = foundUnicode = foundEmoji = foundInjection = foundEmpty = false;

		for(Subcase<String> subcase : subcases) {
			String s = subcase.generate(random);

			if("".equals(s)) {
				foundEmpty = true;
			}
			else if(allCharsAreFrom(s, StringCase.UNICODE_CHARS)) {
				foundUnicode = true;
			}
			else if(allCharsAreFrom(s, StringCase.WHITESPACE_CHARS)) {
				foundWhitespace = true;
			}
			else if(isEmojiString(s)) {
				foundEmoji = true;
			}
			else if(isInjectionString(s)) {
				foundInjection = true;
			}
			else {
				foundStandard = true;
			}
		}

		assertTrue(foundWhitespace);
		assertTrue(foundUnicode);
		assertTrue(foundEmoji);
		assertTrue(foundInjection);
		assertTrue(foundEmpty);
		assertTrue(foundStandard);
	}

	@Test
	public void testWithLengthOf() {
		Case<String> subject = Any.string().withLengthOf(Literal.value(10));
		assertAllSubcases(subject.getSubcases(), s -> s.length() == 10);
	}

	@Test
	public void testWithLength() {
		Case<String> subject = Any.string().withLength(20);
		assertAllSubcases(subject.getSubcases(), s -> s.length() == 20);
	}

	@Test
	public void testWithSourceStringsOf() {
		Case<String> subject = Any.string()
			.withSourceStringsOf(Literal.value(Collections.singleton("HELLO")))
			.withLength(5);

		assertAllSubcases(subject.getSubcases(), "HELLO"::equals);
	}

	@Test
	public void testWithSourceStringsArray() {
		Case<String> subject = Any.string()
			.withSourceStrings("HELLO")
			.withLength(5);

		assertAllSubcases(subject.getSubcases(), "HELLO"::equals);
	}

	@Test
	public void testWithSourceCharsOf() {
		Case<String> subject = Any.string()
			.withSourceCharsOf(Any.of(Literal.value("ABC"), Literal.value("123")))
			.withLength(5);

		Set<String> actuals = subject.getSubcases().stream()
			.map(f -> f.generate(random))
			.collect(Collectors.toSet());

		boolean foundLetters, foundNumbers;
		foundLetters = foundNumbers = false;

		assertEquals(2, actuals.size());

		for(String actual : actuals) {
			assertEquals(5, actual.length());
			if(allCharsAreFrom(actual, FuzzyUtil.toCharSet("ABC"))) {
				foundLetters = true;
			}
			else if(allCharsAreFrom(actual, FuzzyUtil.toCharSet("123"))) {
				foundNumbers = true;
			}
		}

		assertTrue(foundLetters);
		assertTrue(foundNumbers);
	}

	@Test
	public void testWithSourceChars() {
		Case<String> subject = Any.string().withSourceChars("ABC").withLength(100);
		Set<Subcase<String>> subcases = subject.getSubcases();

		assertEquals(1, subcases.size());
		assertAllSubcases(
			subcases,
			s -> allCharsAreFrom(s, FuzzyUtil.toCharSet("ABC"))
		);
	}

	@Test
	public void testWithOnlyAlphabetChars() {
		Case<String> subject = Any.string().withOnlyAlphabetChars().withLength(100);
		Set<Subcase<String>> subcases = subject.getSubcases();

		assertEquals(1, subcases.size());
		assertAllSubcases(
			subcases,
			s -> allCharsAreFrom(s, StringCase.ALPHABET_CHARS)
		);
	}

	@Test
	public void testWithOnlyDigitChars() {
		Case<String> subject = Any.string().withOnlyDigitChars().withLength(100);
		Set<Subcase<String>> subcases = subject.getSubcases();

		assertEquals(1, subcases.size());
		assertAllSubcases(
			subcases,
			s -> allCharsAreFrom(s, StringCase.DIGIT_CHARS)
		);
	}

	@Test
	public void testWithOnlyAlphanumericChars() {
		Case<String> subject = Any.string().withOnlyAlphanumericChars().withLength(100);
		Set<Subcase<String>> subcases = subject.getSubcases();

		assertEquals(1, subcases.size());
		assertAllSubcases(
			subcases,
			s -> allCharsAreFrom(s, FuzzyUtil.union(StringCase.ALPHABET_CHARS, StringCase.DIGIT_CHARS))
		);
	}

	@Test
	public void testWithOnlyHexChars() {
		Case<String> subject = Any.string().withOnlyHexChars().withLength(100);
		Set<Subcase<String>> subcases = subject.getSubcases();

		assertEquals(1, subcases.size());
		assertAllSubcases(
			subcases,
			s -> allCharsAreFrom(s, StringCase.HEX_CHARS)
		);
	}

	@Test
	public void testHandlesNullLengthGracefully() {
		Case<String> subject = Any.string().withLengthOf(Literal.nil());
		assertAllSubcases(subject.getSubcases(), ""::equals);
	}

	@Test
	public void testHandlesLongLengthGracefully() {
		Case<String> subject = Any.string().withLengthOf(Literal.value(5000));
		assertAllSubcases(subject.getSubcases(), s -> s.length() == 1024);
	}

	@Test
	public void testHandlesNegativeLengthGracefully() {
		Case<String> subject = Any.string().withLengthOf(Literal.value(-5000));
		assertAllSubcases(subject.getSubcases(), ""::equals);
	}

	@Test
	public void testHandlesNullSourceStringsGracefully() {
		Case<String> subject = Any.string().withSourceStringsOf(Literal.nil()).withLength(5);
		assertAllSubcases(subject.getSubcases(), "XXXXX"::equals);
	}

	@Test
	public void testHandlesEmptySourceStringsSetGracefully() {
		Case<String> subject = Any.string()
			.withSourceStringsOf(Literal.value(Collections.emptySet()))
			.withLength(5);

		assertAllSubcases(subject.getSubcases(), "XXXXX"::equals);
	}

	@Test
	public void testHandlesEmptySourceStringGracefully() {
		Case<String> subject = Any.string()
			.withSourceStringsOf(Literal.value(Collections.singleton("")))
			.withLength(5);

		assertAllSubcases(subject.getSubcases(), "XXXXX"::equals);
	}

	@Test
	public void testNonEmpty() {
		Set<String> generated = Any.string().nonEmpty().generateAllOnce(random);
		assertFalse(generated.contains(""));
	}

	private static boolean allCharsAreFrom(String str, Set<String> chars) {
		for(int i = 0; i < str.length(); i++) {
			String chr = Character.toString(str.charAt(i));
			if(!chars.contains(chr)) return false;
		}
		return true;
	}

	private static boolean isEmojiString(String str) {
		for(int i = 0; i < str.length() - 1; i += 2) {
			String chr = str.substring(i, i + 2);
			if(!StringCase.EMOJI_CHARS.contains(chr)) return false;
		}
		return true;
	}

	private static boolean isInjectionString(String str) {
		String injection = StringCase.INJECTION_STRINGS.stream().findFirst().get();
		for(int i = 0; i < str.length() - injection.length(); i += injection.length()) {
			String chr = str.substring(i, i + injection.length());
			if(!injection.equals(chr)) return false;
		}
		return true;
	}

	private void assertAllSubcases(Set<Subcase<String>> subcases, Predicate<String> predicate) {
		for(Subcase<String> subcase : subcases) {
			String s = subcase.generate(random);
			assertTrue("Expected predicate to hold for string {" + s + "}", predicate.test(s));
		}
	}

}
