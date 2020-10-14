package org.regenstrief.linkage.util;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

/*
 * Class implements the String matching methods to be used in the
 * record linkage process.  Default thresholds are defined and
 * the methods also support being given a seperate threshold.
 * 
 * JaroWinkler and Levenshtein is implemented by the Simmetrics
 * library jar.  Longest common substring is implemented in
 * a class in this package, and exact match uses the built-in
 * equals Object method
 */

public class StringMatch {
	
	// threshold values for valid matches
	public final static double JWC_THRESH = 0.8;
	
	public final static double LCS_THRESH = 0.85;
	
	public final static double LEV_THRESH = 0.7;
	
	public final static double DICE_THRESH = 0.8;
	
	public static boolean exacthMatch(String str1, String str2, double threshold) {
		return str1.equals(str2) && str1.length() > 0;
	}
	
	public static boolean JWCMatch(String str1, String str2, double threshold) {
		JaroWinkler jwc = new JaroWinkler();
		float thresh = jwc.getSimilarity(str1, str2);
		return thresh > threshold;
	}
	
	public static boolean LCSMatch(String str1, String str2, double threshold) {
		float thresh = LongestCommonSubString.getSimilarity(str1, str2);
		return thresh > threshold;
	}
	
	public static boolean LEVMatch(String str1, String str2, double threshold) {
		Levenshtein lev = new Levenshtein();
		float thresh = lev.getSimilarity(str1, str2);
		return thresh > threshold;
	}
	
	public static boolean DiceMatch(String str1, String str2, double threshold) {
		return getDiceMatchSimilarity(str1, str2) > threshold;
	}
	
	public static boolean exactMatch(String str1, String str2) {
		return str1.equals(str2) && str1.length() > 0;
	}
	
	public static boolean JWCMatch(String str1, String str2) {
		return JWCMatch(str1, str2, JWC_THRESH);
	}
	
	public static boolean LCSMatch(String str1, String str2) {
		return LCSMatch(str1, str2, LCS_THRESH);
	}
	
	public static boolean LEVMatch(String str1, String str2) {
		return LEVMatch(str1, str2, LEV_THRESH);
	}
	
	public static boolean DiceMatch(String str1, String str2) {
		return DiceMatch(str1, str2, DICE_THRESH);
	}
	
	public static float getExactMatchSimilarity(String str1, String str2) {
		if (str1.equals(str2) && str1.length() > 0)
			return 1;
		else
			return 0;
	}
	
	public static float getJWCMatchSimilarity(String str1, String str2) {
		JaroWinkler jwc = new JaroWinkler();
		float thresh = jwc.getSimilarity(str1, str2);
		return thresh;
	}
	
	public static float getLCSMatchSimilarity(String str1, String str2) {
		float thresh = LongestCommonSubString.getSimilarity(str1, str2);
		return thresh;
	}
	
	public static float getLEVMatchSimilarity(String str1, String str2) {
		Levenshtein lev = new Levenshtein();
		float thresh = lev.getSimilarity(str1, str2);
		return thresh;
	}
	
	public static float getDiceMatchSimilarity(String str1, String str2) {
		final int size = str1.length();
		if (str2.length() != size) {
			throw new IllegalArgumentException(
			        str1 + " and " + str2 + " were different sizes; Dice coefficient requires the same size");
		}
		int h = 0, a = 0, b = 0;
		for (int i = 0; i < size; i++) {
			if (str1.charAt(i) == '1') {
				a++;
				if (str2.charAt(i) == '1') {
					b++;
					h++;
				}
			} else if (str2.charAt(i) == '1') {
				b++;
			}
		}
		final float h2 = (h * 2), ab = (a + b);
		return (ab == 0) ? 0 : (h2 / ab);
	}
}
