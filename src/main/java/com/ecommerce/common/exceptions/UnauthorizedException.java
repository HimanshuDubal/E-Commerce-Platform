package com.ecommerce.common.exceptions;

public class UnauthorizedException extends RuntimeException{

	public UnauthorizedException(String message) {
        super(message);
    }
	
	public UnauthorizedException() {
		// TODO Auto-generated constructor stub
	}

}
