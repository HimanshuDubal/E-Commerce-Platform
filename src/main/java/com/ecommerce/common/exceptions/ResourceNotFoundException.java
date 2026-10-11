package com.ecommerce.common.exceptions;

public class ResourceNotFoundException extends RuntimeException{

	public ResourceNotFoundException(String message) {
        super(message);
    }
	
	public ResourceNotFoundException() {
		// TODO Auto-generated constructor stub
	}

}
