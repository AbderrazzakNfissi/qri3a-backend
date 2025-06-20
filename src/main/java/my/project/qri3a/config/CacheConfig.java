package my.project.qri3a.config;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.verification.block-duration-minutes:30}")
    private int blockDurationMinutes;
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("verificationAttempts"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(blockDurationMinutes, TimeUnit.MINUTES)
                .maximumSize(10000));
        return cacheManager;
    }
}