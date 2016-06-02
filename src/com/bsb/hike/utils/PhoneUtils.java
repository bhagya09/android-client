package com.bsb.hike.utils;

import android.database.DatabaseUtils;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Collection;

/**
 * Common utils related to phone numbers
 */
public class PhoneUtils {

    public static boolean isIndianMobileNumber(@NonNull final String number) {

        if (TextUtils.isEmpty(number)) return false;

        // 13 is the number of chars in the phone msisdn
        return (number.startsWith("+919") || number.startsWith("+918") || number.startsWith("+917")) && number.length() == 13;
    }

    public static boolean isIndianNumber(@NonNull final String number) {

        if (TextUtils.isEmpty(number)) return false;

        // 13 is the number of chars in the phone msisdn
        return number.startsWith("+91");
    }

    public static boolean validateBotMsisdn(@NonNull final String msisdn) {
        return !TextUtils.isEmpty(msisdn) && msisdn.startsWith("+");
    }

    public static String normalizeNumber(@NonNull final String inputNumber, @NonNull final String countryCode) {
        if (inputNumber.startsWith("+")) {
            return inputNumber;
        } else if (inputNumber.startsWith("00")) {
            /*
             * Doing for US numbers
             */
            return inputNumber.replaceFirst("00", "+");
        } else if (inputNumber.startsWith("0")) {
            return inputNumber.replaceFirst("0", countryCode);
        } else {
            return countryCode + inputNumber;
        }
    }

    public static String getMsisdnStatement(@NonNull final Collection<String> msisdnList) {
        if (Utils.isEmpty(msisdnList)) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder("(");
            for (String msisdn : msisdnList) {
                sb.append(DatabaseUtils.sqlEscapeString(msisdn));
                sb.append(",");
            }
            int idx = sb.lastIndexOf(",");
            if (idx >= 0)
                sb.replace(idx, sb.length(), ")");
            else
                sb.append(")");
            return sb.toString();
        }
    }
}
