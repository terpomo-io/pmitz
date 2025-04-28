package io.terpomo.pmitz.remote.server.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

public class ApiKeyAuthenticationFilter extends GenericFilterBean {

	private final AuthenticationFailureHandler failureHandler = new AuthenticationEntryPointFailureHandler(
			new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

	private final AuthenticationService authenticationService;

	public ApiKeyAuthenticationFilter(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		try {
			Authentication authentication = authenticationService.getAuthentication((HttpServletRequest) request);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (AuthenticationException exp) {
			failureHandler.onAuthenticationFailure((HttpServletRequest) request, (HttpServletResponse) response, exp);
		}
	}

}
