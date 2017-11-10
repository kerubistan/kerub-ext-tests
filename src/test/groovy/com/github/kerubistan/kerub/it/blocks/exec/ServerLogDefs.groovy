package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given

class ServerLogDefs {

	Scenario scenario  = null

	Map<String, String> logFiles = new HashMap<>()

	@Before
	void setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Given("we will attach the following log files at the end of the scenario")
	void attachLogFiles(DataTable table) {
		table.gherkinRows.forEach {
			logFiles.put(it.cells[0], it.cells[1])
		}
	}

	@After(order = Integer.MAX_VALUE)
	void getLogs() {
		def client = SshUtil.createSshClient()
		logFiles.entrySet().forEach {
			def session = SshUtil.loginWithTestUser(client, it.key)

			scenario.write("attaching ${it.key}:${it.value}")
			scenario.embed(
					session.executeRemoteCommand("sudo cat ${it.value}").getBytes("UTF-16"),
					"text/plain"
			)
		}
		client.stop()
	}
}
