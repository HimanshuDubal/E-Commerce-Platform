package com.ecommerce.address;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.User;
import com.ecommerce.common.exceptions.ResourceNotFoundException;
import com.ecommerce.common.exceptions.UnauthorizedException;


@Service
public class AddressService {

	@Autowired
	private AddressRepository addressRepository;
	
	 public List<Address> getAddressesForUser(User user) {
	        return addressRepository.findByUserId(user.getId());
	    }

	    @Transactional
	    public Address createAddress(User user, Address address) {
	        address.setUser(user);
	        if (Boolean.TRUE.equals(address.getIsDefault())) {
	            unsetExistingDefault(user);
	        }
	        return addressRepository.save(address);
	    }

	    @Transactional
	    public Address updateAddress(User user, Long id, Address updated) {
	        Address existing = getOwnedAddress(user, id);
	        existing.setLine1(updated.getLine1());
	        existing.setCity(updated.getCity());
	        existing.setState(updated.getState());
	        existing.setZipCode(updated.getZipCode());
	        if (Boolean.TRUE.equals(updated.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
	            unsetExistingDefault(user);
	            existing.setIsDefault(true);
	        } else {
	            existing.setIsDefault(updated.getIsDefault());
	        }
	        return addressRepository.save(existing);
	    }

	    @Transactional
	    public void deleteAddress(User user, Long id) {
	        Address address = getOwnedAddress(user, id);
	        addressRepository.delete(address);
	    }

	    private Address getOwnedAddress(User user, Long id) {
	        Address address = addressRepository.findById(id)
	                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
	        if (!address.getUser().getId().equals(user.getId())) {
	            throw new UnauthorizedException("Not authorized");
	        }
	        return address;
	    }

	    private void unsetExistingDefault(User user) {
	        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
	                .ifPresent(defaultAddr -> {
	                    defaultAddr.setIsDefault(false);
	                    addressRepository.save(defaultAddr);
	                });
	    }
	    public AddressService() {
		// TODO Auto-generated constructor stub
	    }

}
