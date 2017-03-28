package com.github.kerubistan.kerub.it.utils

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.digest.BuiltinDigests

import java.security.KeyPair
import java.security.PublicKey

class SshUtil {
	static SshClient createSshClient() {
		def client = SshClient.setUpDefaultClient()
		client.start()
		client
	}

	static ClientSession loginWithTestUser(SshClient client, String address) {
		def connectFuture = client.connect("kerub-test", address, 22)
		connectFuture.await()
		def session = connectFuture.getSession()

		session.addPasswordIdentity("password")
		def authFuture = session.auth()
		authFuture.await()
		authFuture.verify()

		session
	}

	static digest = BuiltinDigests.md5.create()

	static String getSshFingerPrint(PublicKey key) {
		def fingerPrint = KeyUtils.getFingerPrint(digest, key)
		fingerPrint.substring(fingerPrint.indexOf("MD5:"))
	}
}
