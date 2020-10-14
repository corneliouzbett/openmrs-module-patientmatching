package org.regenstrief.linkage.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.regenstrief.linkage.Record;
import org.regenstrief.linkage.io.FormPairs;
import org.regenstrief.linkage.io.LookupFormPairs;
import org.regenstrief.linkage.util.LoggingObject;
import org.regenstrief.linkage.util.MatchingConfig;
import org.regenstrief.linkage.util.MatchingConfigRow;
import org.regenstrief.linkage.util.StringMatch;

/**
 * Class analyzes two record sources to calculate new u values to use in matching. To do this, it
 * creates record pairs based on the blocking columns. It then randomly pairs records from both data
 * sources and compares the demographics, calculating the new u values based on what percent the
 * random pairings match.
 */

public class RandomSampleAnalyzer extends RecordPairAnalyzer {
	
	public static final int SAMPLE_SIZE = 100000;
	
	FormPairs fp;
	
	Random rand;
	
	Hashtable<Integer, List<Integer>> left_pair_entry;
	
	Hashtable<Integer, List<Integer>> right_pair_entry;
	
	Hashtable<Integer, Record> record_pairs;
	
	Hashtable<String, Integer> demographic_agree_count;
	
	boolean[] sample1;
	
	boolean[] sample2;
	
	int pair_count;
	
	/*
	 * Random sampling size as defined by the user that will replace the static value
	 */
	protected int sampleSize;
	
	public RandomSampleAnalyzer(MatchingConfig mc, FormPairs fp) {
		super(mc);
		this.fp = fp;
		initAnalyzer();
	}
	
	protected void initAnalyzer() {
		System.out.println("arrived ????");
		rand = new Random();
		
		left_pair_entry = new Hashtable<Integer, List<Integer>>();
		right_pair_entry = new Hashtable<Integer, List<Integer>>();
		record_pairs = new Hashtable<Integer, Record>();
		demographic_agree_count = new Hashtable<String, Integer>();
		
		int recordPairCount;
		if (fp instanceof LookupFormPairs) {
			recordPairCount = ((LookupFormPairs) fp).size();
		} else {
			recordPairCount = countRecordPairs();
		}
		
		System.out.println("pair count: " + recordPairCount);
		
		sampleSize = mc.getRandomSampleSize();
		sample1 = new boolean[recordPairCount];
		sample2 = new boolean[recordPairCount];
		
		setIndexPairs(recordPairCount);
		
		pair_count = 0;
	}
	
	public void analyzeRecordPair(Record[] pair) {
		if (fp instanceof LookupFormPairs) {
			return;
		}
		
		if (pair_count < sample1.length && sample1[pair_count]) {
			List<Integer> indexes = left_pair_entry.get(Integer.valueOf(pair_count));
			for (int i = 0; i < indexes.size(); i++) {
				int index = indexes.get(i).intValue();
				Record r = record_pairs.get(index);
				if (r == null) {
					record_pairs.put(Integer.valueOf(index), pair[0]);
				} else {
					// compare and increment count hash, then clear
					checkSimilarity(pair[0], r);
					//record_pairs.remove(Integer.valueOf(index));
				}
			}
			//left_pair_entry.remove(Integer.valueOf(pair_count));
		}
		
		if (pair_count < sample2.length && sample2[pair_count]) {
			List<Integer> indexes2 = right_pair_entry.get(Integer.valueOf(pair_count));
			for (int i = 0; i < indexes2.size(); i++) {
				int index = indexes2.get(i).intValue();
				Record r = record_pairs.get(Integer.valueOf(index));
				if (r == null) {
					record_pairs.put(Integer.valueOf(index), pair[1]);
				} else {
					// compare and increment count hash, then clear
					checkSimilarity(pair[1], r);
					//record_pairs.remove(Integer.valueOf(index));
				}
			}
			//right_pair_entry.remove(Integer.valueOf(pair_count));
		}
		pair_count++;
	}
	
	protected void checkSimilarity(Record r1, Record r2) {
		// compare the two records to modify u values
		HashSet<String> demographics = new HashSet<String>();
		List<MatchingConfigRow> includedColumn = mc.getIncludedColumns();
		Iterator<MatchingConfigRow> mcrIterator = includedColumn.iterator();
		while (mcrIterator.hasNext()) {
			MatchingConfigRow mcr = mcrIterator.next();
			demographics.add(mcr.getName());
		}
		
		Iterator<String> it = demographics.iterator();
		while (it.hasNext()) {
			String demographic = it.next();
			Integer bucket = demographic_agree_count.get(demographic);
			if (bucket == null) {
				// not keeping stats for this column yet
				demographic_agree_count.put(demographic, Integer.valueOf(0));
			}
			
			if (matchesOnDemographic(r1, r2, demographic, mc)) {
				Integer non_match_count = demographic_agree_count.get(demographic);
				demographic_agree_count.put(demographic, Integer.valueOf(non_match_count.intValue() + 1));
			}
			
		}
	}
	
	public void finishAnalysis() {
		// review totals and calculate u values
		// modify the matching config object to reflect calculated values
		Iterator<String> it = demographic_agree_count.keySet().iterator();
		while (it.hasNext()) {
			String demographic = it.next();
			int agree_count = demographic_agree_count.get(demographic);
			double u_val = (double) agree_count / (double) sampleSize;
			boolean low_threshold = false;
			if (u_val < MatchingConfig.META_ZERO) {
				// set u to 3 * 1/sample size
				//u_val = MatchingConfig.META_ZERO;
				u_val = 3 * ((double) 1 / (double) sampleSize);
				low_threshold = true;
			} else if (u_val > MatchingConfig.META_ONE) {
				u_val = MatchingConfig.META_ONE;
			}
			
			double stdDev = getStandardDeviation(sampleSize, u_val);
			double[] confidenceInterval = getConfidenceInterval(u_val, stdDev);
			if (low_threshold) {
				log.info("*" + demographic + "," + formatOutput(u_val, stdDev, confidenceInterval));
			} else {
				log.info(demographic + "," + formatOutput(u_val, stdDev, confidenceInterval));
			}
			mc.getMatchingConfigRowByName(demographic).setNonAgreement(u_val);
		}
	}
	
	protected String formatOutput(double u, double stdDev, double[] interval) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(u).append(", ");
		buffer.append(stdDev).append(", ");
		buffer.append("(").append(interval[0]).append(", ");
		buffer.append(interval[1]).append(")");
		return buffer.toString();
	}
	
	/*
	 * Generate standard deviation value
	 * Probably should move this to a static utility class
	 */
	protected double getStandardDeviation(double n, double p) {
		return Math.sqrt(n * p * (1 - p)) / n;
	}
	
	/*
	 * Generate the confidence interval value.
	 * Probably should move this to a static utility class
	 */
	protected double[] getConfidenceInterval(double p, double std) {
		double[] d = new double[2];
		d[0] = p - 2 * std;
		d[1] = p + 2 * std;
		return d;
	}
	
	protected boolean matchesOnDemographic(Record r1, Record r2, String demographic, MatchingConfig mc) {
		String val1 = r1.getDemographic(demographic);
		String val2 = r2.getDemographic(demographic);
		
		MatchingConfigRow mcr = mc.getMatchingConfigRowByName(demographic);
		
		boolean match = false;
		if (val1 != null && val2 != null) {
			switch (mcr.getAlgorithm()) {
				case (MatchingConfig.EXACT_MATCH):
					match = StringMatch.exactMatch(val1, val2);
					break;
				case (MatchingConfig.JWC):
					match = StringMatch.JWCMatch(val1, val2);
					break;
				case (MatchingConfig.LCS):
					match = StringMatch.LCSMatch(val1, val2);
					break;
				case (MatchingConfig.LEV):
					match = StringMatch.LEVMatch(val1, val2);
					break;
				case (MatchingConfig.DICE):
					match = StringMatch.DiceMatch(val1, val2);
					break;
			}
		}
		
		return match;
	}
	
	protected int countRecordPairs() {
		
		int pair_count = 0;
		while (fp.getNextRecordPair() != null) {
			pair_count++;
		}
		
		return pair_count;
	}
	
	protected void setIndexPairs(int max_index) {
		LookupFormPairs lfp = null;
		Hashtable<Integer, List<Integer>> pairs = null;
		if (fp instanceof LookupFormPairs) {
			lfp = (LookupFormPairs) fp;
			pairs = new Hashtable<Integer, List<Integer>>();
		}
		
		// need to get two sets of random numbers, one for each data source
		for (int i = 0; i < sampleSize && max_index > 0; i++) {
			int left_index = rand.nextInt(max_index);
			int right_index = rand.nextInt(max_index);
			
			if (lfp != null) {
				// save index pairs
				int first_index;
				if (left_index < right_index) {
					first_index = left_index;
				} else {
					first_index = right_index;
				}
				List<Integer> l = pairs.get(first_index);
				if (l == null) {
					l = new ArrayList<Integer>();
					pairs.put(first_index, l);
				}
			} else {
				// block to set indexes when we don't have a LookupFormPairs
				sample1[left_index] = true;
				sample2[right_index] = true;
				
				List<Integer> left = left_pair_entry.get(left_index);
				if (left == null) {
					left = new ArrayList<Integer>();
					left_pair_entry.put(left_index, left);
				}
				left.add(i);
				
				List<Integer> right = right_pair_entry.get(right_index);
				if (right == null) {
					right = new ArrayList<Integer>();
					right_pair_entry.put(right_index, right);
				}
				right.add(i);
			}
		}
		
		if (lfp != null) {
			// since we have a LookupFormPairs, we can checkSimilarity right now
			Iterator<Integer> it = pairs.keySet().iterator();
			Record r1;
			while (it.hasNext()) {
				int first_index = it.next();
				List<Integer> l = pairs.get(first_index);
				Collections.sort(l);
				r1 = lfp.getRecordPair(first_index)[0];
				Iterator<Integer> it2 = l.iterator();
				int prev_index = -1;
				Record r2 = null;
				while (it2.hasNext()) {
					int second_index = it2.next();
					if (second_index != prev_index) {
						r2 = lfp.getRecordPair(second_index)[1];
					}
					checkSimilarity(r1, r2);
					prev_index = second_index;
				}
			}
		}
	}
}
