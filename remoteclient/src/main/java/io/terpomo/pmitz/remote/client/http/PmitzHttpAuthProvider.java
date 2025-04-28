package io.terpomo.pmitz.remote.client.http;

import java.util.Map;

/**
 * Authentication Provider for the Http Client
 */
public interface PmitzHttpAuthProvider {

	Map<String, String> getAuthenticationHeaders();
}
