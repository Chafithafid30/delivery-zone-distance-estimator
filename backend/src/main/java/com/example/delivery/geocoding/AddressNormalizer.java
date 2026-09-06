package com.example.delivery.geocoding;

import java.util.Locale;

public final class AddressNormalizer {
    private AddressNormalizer() {}

    public static String normalize(String address) {
        // Preserve punctuation and diacritics: don't merge distinct real addresses.
        return address.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
