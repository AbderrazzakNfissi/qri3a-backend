package my.project.qri3a.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserAuthProvider userAuthProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new JwtAuthFilter(userAuthProvider), BasicAuthenticationFilter.class)
                .sessionManagement(cusomizer -> cusomizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests ->

                        requests.requestMatchers(HttpMethod.POST,
                                        "/api/v1/auth/login","/api/v1/auth/register")
                                .permitAll()
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-resources/**",
                                        "/webjars/**",
                                        "/docs",
                                        "/api-docs/swagger-config",
                                        "/api-docs"
                                ).permitAll()
                                .anyRequest().authenticated());
        return http.build();
    }

}