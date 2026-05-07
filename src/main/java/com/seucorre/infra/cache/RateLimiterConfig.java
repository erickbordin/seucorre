package com.seucorre.infra.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seucorre.shared.api.ApiResponse;
import com.seucorre.usuario.domain.Usuario;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimiterConfig {

    private static final String IA_GENERATION_PATH = "/api/planos/gerar";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:ia:";

    private final ObjectMapper objectMapper;
    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${seucorre.rate-limit.ia.capacity:5}")
    private long capacity;

    @Value("${seucorre.rate-limit.ia.refill-tokens:5}")
    private long refillTokens;

    @Value("${seucorre.rate-limit.ia.refill-minutes:60}")
    private long refillMinutes;

    public RateLimiterConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimiterRedisClient() {
        if (redisUrl != null && !redisUrl.isBlank()) {
            return RedisClient.create(redisUrl);
        }

        RedisURI redisUri = RedisURI.Builder.redis(redisHost, redisPort).build();
        return RedisClient.create(redisUri);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimiterRedisConnection(RedisClient rateLimiterRedisClient) {
        return rateLimiterRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public LettuceBasedProxyManager<String> rateLimiterProxyManager(
            StatefulRedisConnection<String, byte[]> rateLimiterRedisConnection
    ) {
        return LettuceBasedProxyManager.builderFor(rateLimiterRedisConnection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(refillMinutes)))
                .build();
    }

    @Bean
    public HandlerInterceptor iaRateLimiterInterceptor(LettuceBasedProxyManager<String> rateLimiterProxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(refillTokens, Duration.ofMinutes(refillMinutes)))
                .build();

        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }

                Bucket bucket = rateLimiterProxyManager.getProxy(resolverChaveRateLimit(request), () -> configuration);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                if (probe.isConsumed()) {
                    return true;
                }

                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.setHeader(
                        "X-Rate-Limit-Retry-After-Seconds",
                        String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()))
                );

                objectMapper.writeValue(
                        response.getWriter(),
                        ApiResponse.erro("Limite de requisições para geração de IA excedido. Tente novamente mais tarde.")
                );
                return false;
            }
        };
    }

    @Bean
    public WebMvcConfigurer rateLimiterWebMvcConfigurer(HandlerInterceptor iaRateLimiterInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(iaRateLimiterInterceptor)
                        .addPathPatterns(IA_GENERATION_PATH);
            }
        };
    }

    private String resolverChaveRateLimit(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            if (usuario.getId() != null) {
                return RATE_LIMIT_KEY_PREFIX + usuario.getId();
            }
            if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
                return RATE_LIMIT_KEY_PREFIX + usuario.getEmail();
            }
        }

        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return RATE_LIMIT_KEY_PREFIX + authentication.getName();
        }

        return RATE_LIMIT_KEY_PREFIX + request.getRemoteAddr();
    }
}
