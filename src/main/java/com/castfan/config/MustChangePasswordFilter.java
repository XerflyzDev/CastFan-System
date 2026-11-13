package com.castfan.config;

import java.io.IOException;

import com.castfan.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {
	private final UserAccountRepository accounts;

	public MustChangePasswordFilter(UserAccountRepository accounts) { this.accounts = accounts; }

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		var path = request.getRequestURI().substring(request.getContextPath().length());
		var allowed = path.equals("/account/change-password") || path.equals("/logout")
			|| path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/fonts/") || path.startsWith("/images/");
		if (!allowed && authentication != null && authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken)) {
			var mustChange = accounts.findByLoginIdentifierIgnoreCase(authentication.getName())
				.map(account -> account.isMustChangePassword()).orElse(false);
			if (mustChange) {
				response.sendRedirect(request.getContextPath() + "/account/change-password");
				return;
			}
		}
		chain.doFilter(request, response);
	}
}
