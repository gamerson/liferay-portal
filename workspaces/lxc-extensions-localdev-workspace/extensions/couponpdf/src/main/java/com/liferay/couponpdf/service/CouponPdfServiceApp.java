package com.liferay.couponpdf.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class CouponPdfServiceApp {

  public static void main(String[] args) {
    System.getenv().entrySet().stream()
        .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .forEach(System.out::println);

    SpringApplication.run(CouponPdfServiceApp.class, args);
  }
}
