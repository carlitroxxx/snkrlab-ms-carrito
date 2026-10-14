package com.snkrlab.carrito.config;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.nimbusds.jwt.JWTParser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.tenant-id}")
    private String tenantId;

    @Value("${app.seguridad.azure-client-id}")
    private String azureClientId;

    @Value("${app.seguridad.cognito-issuer}")
    private String cognitoIssuer;

    @Value("${app.seguridad.cognito-client-id}")
    private String cognitoClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        String azureIssuerV1 =
                "https://sts.windows.net/" + tenantId + "/";

        String azureIssuerV2 =
                "https://login.microsoftonline.com/"
                        + tenantId + "/v2.0";

        String azureKeys =
                "https://login.microsoftonline.com/"
                        + tenantId + "/discovery/v2.0/keys";

        NimbusJwtDecoder azureV1 =
                crearDecoderAzure(azureIssuerV1, azureKeys);

        NimbusJwtDecoder azureV2 =
                crearDecoderAzure(azureIssuerV2, azureKeys);

        NimbusJwtDecoder cognito =
                NimbusJwtDecoder.withJwkSetUri(
                        cognitoIssuer + "/.well-known/jwks.json"
                ).build();

        OAuth2TokenValidator<Jwt> usoCognito =
                new JwtClaimValidator<String>(
                        "token_use",
                        valor -> "access".equals(valor)
                );

        OAuth2TokenValidator<Jwt> clienteCognito =
                new JwtClaimValidator<String>(
                        "client_id",
                        valor -> cognitoClientId.equals(valor)
                );

        cognito.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(
                        JwtValidators.createDefaultWithIssuer(cognitoIssuer),
                        usoCognito,
                        clienteCognito
                )
        );

        Map<String, JwtDecoder> emisoresPermitidos = Map.of(
                azureIssuerV1, azureV1,
                azureIssuerV2, azureV2,
                cognitoIssuer, cognito
        );

        return token -> {
            String emisor;

            try {
                emisor = JWTParser.parse(token)
                        .getJWTClaimsSet()
                        .getIssuer();
            } catch (ParseException | IllegalArgumentException e) {
                throw new BadJwtException(
                        "El token no tiene un formato válido."
                );
            }

            if (emisor == null) {
                throw new BadJwtException(
                        "El token no contiene emisor."
                );
            }

            JwtDecoder decoder = emisoresPermitidos.get(emisor);

            if (decoder == null) {
                throw new BadJwtException(
                        "El emisor del token no está permitido."
                );
            }

            return decoder.decode(token);
        };
    }

    private NimbusJwtDecoder crearDecoderAzure(
            String emisor,
            String claves) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(claves).build();

        OAuth2TokenValidator<Jwt> audiencia =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        valores -> valores != null
                                && (
                                valores.contains(azureClientId)
                                        || valores.contains("api://" + azureClientId)
                        )
                );

        OAuth2TokenValidator<Jwt> permisoApi =
                new JwtClaimValidator<String>(
                        "scp",
                        valor -> valor != null
                                && List.of(valor.split(" "))
                                .contains("write-read")
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(
                        JwtValidators.createDefaultWithIssuer(emisor),
                        audiencia,
                        permisoApi
                )
        );

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String emisor = jwt.getClaimAsString("iss");

            if (cognitoIssuer.equals(emisor)) {
                List<String> grupos =
                        jwt.getClaimAsStringList("cognito:groups");

                if (grupos != null && grupos.contains("Cliente")) {
                    return List.<GrantedAuthority>of(
                            new SimpleGrantedAuthority("ROLE_Cliente")
                    );
                }

                return List.<GrantedAuthority>of();
            }

            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles == null) {
                return List.<GrantedAuthority>of();
            }

            return roles.stream()
                    .filter(rol ->
                            "Cliente".equals(rol)
                                    || "Administrador".equals(rol)
                    )
                    .<GrantedAuthority>map(rol ->
                            new SimpleGrantedAuthority("ROLE_" + rol)
                    )
                    .toList();
        });

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of("*", "http://localhost:5173")
        );

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().hasRole("Cliente")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }
}