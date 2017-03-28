package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.java.en.Given
import org.slf4j.LoggerFactory

import java.nio.charset.Charset

class ExecDefs {

	private static final logger = LoggerFactory.getLogger(ExecDefs)

	@Given("command executed on (\\S+):(.*)")
	void executeOnNode(String nodeAddress, String command) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def out = new ByteArrayOutputStream()
		session.executeRemoteCommand(command, out, Charset.forName("ASCII"))
		logger.info("out: ${new String(out.toByteArray())}" )

		session.close()

	}
}
