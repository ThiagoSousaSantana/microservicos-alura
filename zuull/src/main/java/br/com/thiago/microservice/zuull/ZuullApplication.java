package br.com.thiago.microservice.zuull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
public class ZuullApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuullApplication.class, args);
	}

}
