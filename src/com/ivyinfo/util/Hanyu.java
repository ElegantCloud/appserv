package com.ivyinfo.util;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class Hanyu {
	private static Hanyu instance;
	private HanyuPinyinOutputFormat format = null;

	private String[] pinyin;

	private List<String> fullPinyins;
	private List<String> shortPinyins;

	private Hanyu() {

		format = new HanyuPinyinOutputFormat();

		format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		format.setCaseType(HanyuPinyinCaseType.LOWERCASE);

		pinyin = null;
	}

	public static Hanyu getInstance() {
		if (instance == null) {
			instance = new Hanyu();
		}
		return instance;
	}

	public void processString(String str) {
		fullPinyins = new ArrayList<String>();
		shortPinyins = new ArrayList<String>();

		fullPinyins.add("");
		shortPinyins.add("");

		char lastCH = ' ';
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			// System.out.println("char: " + ch);
			if (ch >= 'A' && ch <= 'Z') {
				ch = (char) ((byte) ch + 32);
			}
			String[] tempPinyins = getCharacterPinYin(ch);
			if (tempPinyins == null) {
				if (ch != ' ' && ch != '.') {
					int size = fullPinyins.size();
					for (int j = 0; j < size; j++) {
						String full = fullPinyins.remove(0);
						full = full + ch;
						fullPinyins.add(full);

						if (lastCH == ' ' || lastCH == '.') {
							String fc = shortPinyins.remove(0);
							fc = fc + ch;
							shortPinyins.add(fc);
						}
					}
				}
				lastCH = ch;

			} else {
				// System.out.println("pinyin for " + ch);
				// for (String tempPinyin : tempPinyins) {
				// System.out.println("pinyin: " + tempPinyin);
				// }

				int fullSize = fullPinyins.size();
				int shortSize = shortPinyins.size();

				// System.out.println("full size: " + fullSize + " short size: "
				// + shortSize);

				for (int j = 0; j < fullSize; j++) {
					String full = fullPinyins.remove(0);
					String fc = shortPinyins.remove(0);
					// System.out.println("1 full: " + full + " fc: " + fc);
					for (String tempPinyin : tempPinyins) {
						String tmpFull = full.concat(tempPinyin);
						String tmpFc = fc + tempPinyin.charAt(0);

						// System.out.println("2 full: " + tmpFull + " fc: " +
						// tmpFc);
						fullPinyins.add(tmpFull);
						shortPinyins.add(tmpFc);
					}
				}

			}
		}
	}

	public String[] getCharacterPinYin(char c) {
		try {
			pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format);
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			e.printStackTrace();
			pinyin = null;
		}

		return pinyin;

	}

	/**
	 * get the first characters of pinyin
	 * 
	 * @return
	 */
	public List<String> getShortPinyins() {
		return shortPinyins;
	}

	/**
	 * get the full pinyin word
	 * 
	 * @return
	 */
	public List<String> getFullPinyins() {
		return fullPinyins;
	}

	public static void main(String args[]) {
		String name = "aaa";
		Hanyu hy = new Hanyu();
		hy.processString(name);
		List<String> shortPinyins = hy.getShortPinyins();
		List<String> fullPinyins = hy.getFullPinyins();
		System.out.println("################");
		for (String sp : shortPinyins) {
			System.out.println("Short: " + sp);
		}

		for (String full : fullPinyins) {
			System.out.println("Full: " + full);
		}
	}
}
