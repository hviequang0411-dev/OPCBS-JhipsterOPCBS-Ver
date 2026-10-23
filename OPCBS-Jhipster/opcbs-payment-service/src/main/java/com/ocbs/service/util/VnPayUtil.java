package com.ocbs.service.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnPayUtil {

    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String DEFAULT_TMN_CODE = "OPCBSDEMO";
    public static final String DEFAULT_HASH_SECRET = "OPCBSVNPAYHASHSECRETDEMO202610";
    public static final String DEFAULT_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    // Backward compatibility constants
    public static final String VNP_TMN_CODE = DEFAULT_TMN_CODE;
    public static final String VNP_HASH_SECRET = DEFAULT_HASH_SECRET;
    public static final String VNP_PAY_URL = DEFAULT_PAY_URL;

    private VnPayUtil() {}

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        String key = (secretKey != null && !secretKey.isBlank()) ? secretKey : DEFAULT_HASH_SECRET;
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return hmacSHA512(key, sb.toString());
    }

    public static String hashAllFields(Map<String, String> fields) {
        return hashAllFields(fields, DEFAULT_HASH_SECRET);
    }

    public static boolean verifyCallbackSignature(Map<String, String> vnpParams, String secretKey) {
        String secureHash = vnpParams.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        Map<String, String> paramsToHash = new HashMap<>(vnpParams);
        paramsToHash.remove("vnp_SecureHash");
        paramsToHash.remove("vnp_SecureHashType");

        String calculatedHash = hashAllFields(paramsToHash, secretKey);
        return secureHash.equalsIgnoreCase(calculatedHash);
    }

    public static boolean verifyCallbackSignature(Map<String, String> vnpParams) {
        return verifyCallbackSignature(vnpParams, DEFAULT_HASH_SECRET);
    }
}
