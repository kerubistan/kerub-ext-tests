package com.github.kerubistan.kerub.it.utils

import java.security.KeyPair
import java.security.PrivateKey

import static java.lang.Thread.currentThread
import static java.security.KeyStore.getInstance

class SecurityUtil {

	static KeyPair getNodeKeyPair() {
		def keyStore = getInstance("JKS")
		keyStore.load(currentThread().getContextClassLoader().getResourceAsStream("testnodekey.jks"), "password".toCharArray())
		def key = keyStore.getKey("kerub", "password".toCharArray())
		def cert = keyStore.getCertificate("kerub")

		new KeyPair(cert.publicKey, key as PrivateKey)
	}
}
