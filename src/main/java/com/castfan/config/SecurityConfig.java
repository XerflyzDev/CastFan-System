package com.castfan.config;

import com.castfan.entity.UserStatus;
import com.castfan.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		@Value("${castfan.security.development-endpoints:false}") boolean developmentEndpoints,
		@Value("${castfan.security.require-https:false}") boolean requireHttps
	) throws Exception {
		http
			.authorizeHttpRequests(authorize -> {
				authorize.requestMatchers(
					"/", "/login", "/signup", "/casts/**", "/events/**", "/promotions/**", "/avatars/**",
					"/css/**", "/js/**", "/images/**", "/fonts/**", "/error",
					"/actuator/health", "/actuator/health/**"
				).permitAll();
				authorize.requestMatchers("/actuator/**").denyAll();
				if (developmentEndpoints) {
					authorize.requestMatchers("/concepts/**", "/h2-console/**").permitAll();
				} else {
					authorize.requestMatchers("/concepts/**", "/h2-console/**").denyAll();
				}
				authorize
					.requestMatchers("/member/schedule", "/member/casts/**", "/member/following", "/member/notifications")
					.hasAnyRole("MEMBER", "CAST")
					.requestMatchers("/cast/**").hasRole("CAST")
					.requestMatchers("/owner/**").hasRole("OWNER")
					.requestMatchers("/member/**").hasRole("MEMBER")
					.anyRequest().authenticated();
			})
			.formLogin(form -> form
				.loginPage("/login")
				.usernameParameter("email")
				.successHandler((request, response, authentication) -> {
					var authorities = authentication.getAuthorities().stream()
						.map(authority -> authority.getAuthority())
						.collect(java.util.stream.Collectors.toSet());
					var destination = authorities.contains("ROLE_OWNER")
						? "/"
						: authorities.contains("ROLE_CAST") ? "/cast" : "/account";
					response.sendRedirect(request.getContextPath() + destination);
				})
				.failureUrl("/login?error")
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.permitAll())
			.csrf(csrf -> {
				if (developmentEndpoints) csrf.ignoringRequestMatchers("/h2-console/**");
			})
			.headers(headers -> {
				headers
					.contentTypeOptions(Customizer.withDefaults())
					.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
					.permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"));
				if (developmentEndpoints) {
					headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
				} else {
					headers
						.frameOptions(frameOptions -> frameOptions.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives(
							"default-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; "
								+ "object-src 'none'; script-src 'self'; style-src 'self' https://cdn.jsdelivr.net; "
								+ "font-src 'self' https://cdn.jsdelivr.net data:; img-src 'self' https: data:"
						));
				}
			});

		if (requireHttps) {
			http.redirectToHttps(Customizer.withDefaults());
		}

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
		return email -> userAccountRepository.findByEmailIgnoreCase(email.trim())
			.map(account -> User.withUsername(account.getEmail())
				.password(account.getPasswordHash())
				.roles(account.getRole().name())
				.disabled(account.getStatus() != UserStatus.ACTIVE)
				.build())
			.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
	}
}
