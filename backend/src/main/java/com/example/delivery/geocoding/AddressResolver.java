package com.example.delivery.geocoding;

/** Resolves an address locally or through the shared geocoding service. */
public interface AddressResolver {
    GeocodeResult resolveAddress(String address);
}
