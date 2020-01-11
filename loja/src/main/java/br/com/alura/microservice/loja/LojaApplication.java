package br.com.alura.microservice.loja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.client.RestTemplate;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@SpringBootApplication
@EnableFeignClients
@EnableCircuitBreaker
@EnableScheduling
@EnableResourceServer
public class LojaApplication {
	
	@Bean
	public RequestInterceptor getInterceptorDeAuthentication() {
		return new RequestInterceptor() {
			@Override
			public void apply(RequestTemplate template) {
				var auth = SecurityContextHolder.getContext().getAuthentication();
				if(auth == null) {
					return;
				}
				
				var details = (OAuth2AuthenticationDetails) auth.getDetails();
				template.header("Authorization", "Bearer ".concat(details.getTokenValue()));
			}
		};
	}
	
	@Bean
	@LoadBalanced
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(LojaApplication.class, args);
	}

}
