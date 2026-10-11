package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class CouponApplyRequest {

	@NotBlank
    private String code;
	
	public CouponApplyRequest() {
		// TODO Auto-generated constructor stub
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	

}
