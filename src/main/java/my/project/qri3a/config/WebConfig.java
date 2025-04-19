package my.project.qri3a.config;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    private final SeoInterceptor seoInterceptor;

    public WebConfig(SeoInterceptor seoInterceptor) {
        this.seoInterceptor = seoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Ajouter l'intercepteur SEO pour tous les endpoints d'API
        registry.addInterceptor(seoInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        // Create a CORS configuration object
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);

        // Specify allowed origins. You can add more origins or use patterns if needed
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:4200","http://localhost:53538/","http://192.168.11.115:4200"));

        // Specify allowed headers
        corsConfig.setAllowedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));

        // Specify allowed HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.PATCH.name()
        ));

        // Set how long the response from a pre-flight request can be cached by clients
        corsConfig.setMaxAge(3600L);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        // Create the CorsFilter with the configuration source
        CorsFilter corsFilter = new CorsFilter(source);

        // Register the filter
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(-102);

        return bean;
    }
}