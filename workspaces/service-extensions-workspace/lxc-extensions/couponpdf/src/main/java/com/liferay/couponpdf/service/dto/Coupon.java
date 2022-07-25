package com.liferay.couponpdf.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Coupon {

  public String code;
  public String id;
  public boolean issued;
  public LocalDateTime issueDate;
}
