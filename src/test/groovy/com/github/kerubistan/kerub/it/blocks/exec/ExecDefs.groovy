package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Given

import java.nio.charset.Charset

class ExecDefs {

	Scenario scenario = null

	@Before
	 setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Given("command executed on (\\S+):(.*)")
	void executeOnNode(String nodeAddress, String command) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def err = new ByteArrayOutputStream()
		def out = session.executeRemoteCommand(command, err, Charset.forName("ASCII"))
		def error = new String(err.toByteArray())
		scenario.write(out)
		if(!error.isEmpty()) {
			scenario.write("err: $error")
		}

		session.close()

	}
}
