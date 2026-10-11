package com.ecommerce.common.exceptions;

public class InvalidStateTransitionException extends RuntimeException{

	public InvalidStateTransitionException(String message) {
        super(message);
    }
	public InvalidStateTransitionException() {
		// TODO Auto-generated constructor stub
	}

}
