package com.ask.ats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type ATS scheduler application.
 */
@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@RestController
@EnableAsync
public class AtsSchedulerApplication {
    /**
    * The entry point of application.
    *
    * @param args the input arguments
    */
    public static void main(String[] args) {
        SpringApplication.run(AtsSchedulerApplication.class, args);
    }

}
