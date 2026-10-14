package com.castfan.config;

import com.castfan.entity.UserStatus;
import com.castfan.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/login", "/signup", "/casts/**", "/events/**", "/css/**", "/js/**", "/images/**", "/fonts/**", "/concepts/**", "/error", "/h2-console/**")
				.permitAll()
				.requestMatchers("/cast/**").hasRole("CAST")
				.requestMatchers("/owner/**").hasRole("OWNER")
				.requestMatchers("/member/**").hasRole("MEMBER")
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.usernameParameter("email")
				.defaultSuccessUrl("/account", true)
				.failureUrl("/login?error")
				.permitAll())
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.permitAll())
			.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
			.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

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
