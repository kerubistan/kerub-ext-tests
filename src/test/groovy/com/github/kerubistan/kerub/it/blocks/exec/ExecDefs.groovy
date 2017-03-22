package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SecurityUtil
import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.java.en.Given
import org.slf4j.LoggerFactory

class ExecDefs {

	private static final logger = LoggerFactory.getLogger(ExecDefs)

	@Given("command executed on (\\S+):(.*)")
	void executeOnNode(String nodeAddress, String command) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithPublicKey(client, nodeAddress, SecurityUtil.getNodeKeyPair())

		def execChannel = session.createExecChannel(command)
		execChannel.invertedOut?.readLines()?.forEach { logger.warn("out: $it") }
		execChannel.invertedErr?.readLines()?.forEach { logger.warn("err: $it") }

		session.close()

	}
}
