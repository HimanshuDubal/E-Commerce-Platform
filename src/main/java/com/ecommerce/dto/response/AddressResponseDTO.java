package com.ecommerce.dto.response;

import com.ecommerce.address.Address;

public record AddressResponseDTO(
        Long id,
        String line1,
        String city,
        String state,
        String zipCode,
        Boolean isDefault
) {
    public static AddressResponseDTO from(Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(address.getId(),address.getLine1(), address.getCity(),
                address.getState(), address.getZipCode(), address.getIsDefault());
    }
}
