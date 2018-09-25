package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given

class ServerLogDefs {

	Scenario scenario  = null

	List<Tuple> logFiles = new ArrayList<>()

	@Before
	void setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Given("we will attach the following log files at the end of the scenario")
	void attachLogFiles(DataTable table) {
		table.gherkinRows.forEach {
			logFiles.add(new Tuple(it.cells[0], it.cells[1]))
		}
	}

	@After(order = Integer.MAX_VALUE)
	void getLogs() {
		def client = SshUtil.createSshClient()
		logFiles.forEach {
			def host = it.get(0).toString()
			def files = it.get(1)
			def session = SshUtil.loginWithTestUser(client, host)

			scenario.write("attaching ${host}:${files}")
			scenario.embed(
					session.executeRemoteCommand("sudo cat ${files}").getBytes("UTF-16"),
					"text/plain"
			)
		}
		client.stop()
	}
}
