package com.flyemu.share.way;

import java.util.Random;

/**
 * 生成商品、货商、客户、仓库等业务编号
 */
public class CodeGenerator {

    public enum CodeType {
        PRODUCT("PR"),         // 商品
        SUPPLIER("SU"),         // 货商
        CUSTOMER("CU"),         // 客户
        WAREHOUSE("WH");          // 仓库

        private final String prefix;

        CodeType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private static final int RANDOM_LENGTH = 8;
    private static final Random random = new Random();

    public static String generateCode(CodeType codeType) {
        String prefix = codeType.getPrefix();
        String randomSuffix = generateRandomDigits(RANDOM_LENGTH);      // 随机数部分

        return prefix + randomSuffix;
    }

    private static String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }


}
