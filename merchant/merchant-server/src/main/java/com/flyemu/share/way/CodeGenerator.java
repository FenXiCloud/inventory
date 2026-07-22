package com.flyemu.share.way;

import java.util.Random;

/**
 * 生成商品、货商、客户、仓库等业务编号
 */
public class CodeGenerator {

    private static final int RANDOM_LENGTH = 8;
    private static final Random random = new Random();

    public static String generateCode() {
        String randomSuffix = generateRandomDigits(RANDOM_LENGTH);
        return randomSuffix;
    }

    private static String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

}
