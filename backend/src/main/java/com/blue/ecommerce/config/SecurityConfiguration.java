package com.blue.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.web.accept.ContentNegotiationStrategy;
//import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import static org.springframework.security.config.Customizer.withDefaults;


// import com.okta.spring.boot.oauth.Okta;

@Configuration
public class SecurityConfiguration {

    

    @Bean
SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
    .cors(withDefaults())
    .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
    .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/**").permitAll()
        .requestMatchers("/api/checkout/**").permitAll() 
        .anyRequest().authenticated())
      .formLogin(withDefaults());

    return http.build();
}

}
