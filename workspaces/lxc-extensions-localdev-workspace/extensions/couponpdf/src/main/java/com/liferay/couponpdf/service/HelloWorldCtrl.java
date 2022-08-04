package com.liferay.couponpdf.service;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloWorldCtrl {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}
