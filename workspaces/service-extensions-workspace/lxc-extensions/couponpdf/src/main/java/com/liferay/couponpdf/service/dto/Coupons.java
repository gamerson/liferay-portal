package com.liferay.couponpdf.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Coupons {

  public Coupon[] items;
}
