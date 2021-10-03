package com.zpedroo.voltzmachines.utils.formatter;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class NumberFormatter {

    private static NumberFormatter instance;
    public static NumberFormatter getInstance() { return instance; }

    private BigInteger THOUSAND = BigInteger.valueOf(1000);
    private NavigableMap<BigInteger, String> FORMATS = new TreeMap<>();
    private List<String> NAMES = new LinkedList<>();

    public NumberFormatter(FileConfiguration file) {
        instance = this;
        NAMES.addAll(file.getStringList("Number-Formatter"));

        for (int i = 0; i < NAMES.size(); i++) {
            FORMATS.put(THOUSAND.pow(i+1), NAMES.get(i));
        }
    }

    public BigInteger filter(String str) {
        String onlyNumbers = str.replaceAll("[^0-9]+", "");
        if (onlyNumbers.isEmpty()) return BigInteger.ZERO; // invalid amount

        BigInteger number = new BigInteger(onlyNumbers);

        String onlyLetters = str.replaceAll("[^A-Za-z]+", "");

        int i = -1;
        if (NAMES.contains(onlyLetters)) {
            for (String format : NAMES) {
                ++i;

                if (StringUtils.equals(format, onlyLetters)) break;
            }
        }

        if (i != -1) number = number.multiply(THOUSAND.pow(i + 1));

        return number;
    }

    public String format(BigInteger value) {
        Map.Entry<BigInteger, String> entry = FORMATS.floorEntry(value);
        if (entry == null) return value.toString();

        BigInteger key = entry.getKey();
        BigInteger divide = key.divide(THOUSAND);
        BigInteger divide1 = value.divide(divide);
        float f = divide1.floatValue() / 1000f;
        float rounded = ((int)(f * 100))/100f;

        if (rounded % 1 == 0) return ((int) rounded) + "" + entry.getValue();

        return rounded + "" + entry.getValue();
    }

    public String formatDecimal(Double value) {
        DecimalFormat formatter = new DecimalFormat("#.#");
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator('.');

        formatter.setDecimalFormatSymbols(symbols);

        return formatter.format(value);
    }
}