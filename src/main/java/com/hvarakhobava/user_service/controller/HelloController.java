package com.hvarakhobava.user_service.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author Hanna Varakhobava
 */
@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello";
    }
}
