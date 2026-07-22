package com.flyemu.share.common;

import com.flyemu.share.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/** 拼音工具 */
@Slf4j
public final class PinYinUtil {

    private PinYinUtil() {
    }

    /** 将文字转为汉语拼音 */
    public static String toPinyin(String chineseLanguage) {
        char[] chars = chineseLanguage.trim().toCharArray();
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        try {
            for (char ch : chars) {
                if (String.valueOf(ch).matches("[\u4e00-\u9fa5]+")) {
                    pinyin.append(PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat)[0]);
                } else {
                    pinyin.append(ch);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            log.error(chineseLanguage + "转拼音失败！", e);
            throw new ServiceException(chineseLanguage + "转拼音失败！", e);
        }
        return pinyin.toString();
    }

    public static String getFirstLettersUp(String chineseLanguage) {
        return getFirstLetters(chineseLanguage, HanyuPinyinCaseType.UPPERCASE);
    }

    public static String getFirstLettersLo(String chineseLanguage) {
        return getFirstLetters(chineseLanguage, HanyuPinyinCaseType.LOWERCASE);
    }

    public static String getFirstLetters(String chineseLanguage, HanyuPinyinCaseType caseType) {
        char[] chars = chineseLanguage.trim().toCharArray();
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(caseType);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try {
            for (char ch : chars) {
                String str = String.valueOf(ch);
                if (str.matches("[\u4e00-\u9fa5]+")) {
                    pinyin.append(PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat)[0].substring(0, 1));
                } else if (str.matches("[0-9]+")) {
                    pinyin.append(ch);
                } else if (str.matches("[a-zA-Z]+")) {
                    pinyin.append(ch);
                } else {
                    pinyin.append(ch);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            log.error(chineseLanguage + "转拼音失败！", e);
            throw new ServiceException(chineseLanguage + "转拼音失败！", e);
        }
        return pinyin.toString();
    }

    public static String getPinyinString(String chineseLanguage) {
        char[] chars = chineseLanguage.trim().toCharArray();
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try {
            for (char ch : chars) {
                String str = String.valueOf(ch);
                if (str.matches("[\u4e00-\u9fa5]+")) {
                    pinyin.append(PinyinHelper.toHanyuPinyinStringArray(ch, defaultFormat)[0]);
                } else if (str.matches("[0-9]+")) {
                    pinyin.append(ch);
                } else if (str.matches("[a-zA-Z]+")) {
                    pinyin.append(ch);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            log.error(chineseLanguage + "转拼音失败！", e);
            throw new ServiceException(chineseLanguage + "转拼音失败！", e);
        }
        return pinyin.toString();
    }

    /** 取第一个汉字的第一个拼音字母 */
    public static String getFirstLetter(String chineseLanguage) {
        char[] chars = chineseLanguage.trim().toCharArray();
        String pinyin = "";
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        try {
            String str = String.valueOf(chars[0]);
            if (str.matches("[\u4e00-\u9fa5]+")) {
                pinyin = PinyinHelper.toHanyuPinyinStringArray(chars[0], defaultFormat)[0].substring(0, 1);
            } else if (str.matches("[0-9]+")) {
                pinyin += chars[0];
            } else if (str.matches("[a-zA-Z]+")) {
                pinyin += chars[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            log.error(chineseLanguage + "转拼音失败！", e);
            throw new ServiceException(chineseLanguage + "转拼音失败！", e);
        }
        return pinyin;
    }
}
