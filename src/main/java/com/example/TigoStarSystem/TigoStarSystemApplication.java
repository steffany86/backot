package com.example.TigoStarSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class TigoStarSystemApplication {

	public static void main(String[] args) {
		habilitarCompatibilidadTlsLegacy();
		SpringApplication.run(TigoStarSystemApplication.class, args);
	}

	private static void habilitarCompatibilidadTlsLegacy() {
		String enabled = System.getProperty("app.tls.legacy.enabled", "true");
		if (!"true".equalsIgnoreCase(enabled)) {
			return;
		}
		System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
		Security.setProperty("jdk.tls.disabledAlgorithms", "SSLv3, DTLSv1.0, anon, NULL");
		Security.setProperty("jdk.certpath.disabledAlgorithms", "");
	}
}
