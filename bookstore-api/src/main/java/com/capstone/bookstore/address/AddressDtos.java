package com.capstone.bookstore.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AddressDtos {

    private AddressDtos() {}

    public record AddressRequest(
            @Size(max = 50) String label,
            @NotBlank @Size(max = 150) String recipientName,
            @NotBlank @Size(max = 255) String line1,
            @Size(max = 255) String line2,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 100) String state,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{3,20}$", message = "must be a valid postal code")
            String postalCode,
            @NotBlank @Size(max = 100) String country,
            @Pattern(regexp = "^[+0-9 -]{7,20}$", message = "must be a valid phone number") String phone,
            Boolean isDefault) {}

    public record AddressResponse(Long id, String label, String recipientName, String line1, String line2,
                                  String city, String state, String postalCode, String country, String phone,
                                  boolean isDefault) {

        public static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getLine1(), a.getLine2(),
                    a.getCity(), a.getState(), a.getPostalCode(), a.getCountry(), a.getPhone(),
                    a.isDefaultAddress());
        }
    }
}
