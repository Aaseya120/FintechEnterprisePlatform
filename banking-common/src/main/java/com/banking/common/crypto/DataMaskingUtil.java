package com.banking.common.crypto;

public final class DataMaskingUtil {

    private DataMaskingUtil() {}

    /**
     * PCI-DSS PAN Masking: Shows first 4 and last 4 digits.
     * Example: 4111222233334444 -> 4111-XXXX-XXXX-4444
     */
    public static String maskCardPan(String pan) {
        if (pan == null || pan.length() < 8) return "XXXX";
        String clean = pan.replaceAll("\\s+|-", "");
        int len = clean.length();
        return clean.substring(0, 4) + "-XXXX-XXXX-" + clean.substring(len - 4);
    }

    public static String maskCardNumber(String pan) {
        return maskCardPan(pan);
    }

    /**
     * Account Number Masking: Shows first 2 characters and last 4 digits.
     * Example: US1234567890 -> USXXXXXX7890
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 6) return "XXXXXX";
        int len = accountNumber.length();
        return accountNumber.substring(0, 2) + "X".repeat(len - 6) + accountNumber.substring(len - 4);
    }

    /**
     * Email Masking for GDPR Compliance.
     * Example: john.doe@bank.com -> j***e@bank.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }

    /**
     * SSN / National ID Masking.
     * Example: 123-45-6789 -> XXX-XX-6789
     */
    public static String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 4) return "XXX-XX-XXXX";
        String clean = ssn.replaceAll("\\s+|-", "");
        int len = clean.length();
        return "XXX-XX-" + clean.substring(len - 4);
    }
}
