package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import io.cucumber.datatable.DataTable
import org.apache.sshd.common.RuntimeSshException

import java.rmi.RemoteException

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

class LogEnvironment {
	List<Tuple> logFiles = new ArrayList<>()
}

World {
	new LogEnvironment()
}

Given(~/^we will attach the following log files at the end of the scenario$/) {
	DataTable table ->
		def _logFiles = logFiles
		table.cells().forEach {
			_logFiles.add(new Tuple(it[0], it[1]))
		}
}

After(order = Integer.MAX_VALUE) {
	def client = SshUtil.createSshClient()
	logFiles.forEach {
		def host = it.get(0).toString()
		def files = it.get(1)
		try {
			def session = SshUtil.loginWithTestUser(client, host)

			try {
				scenario.write("attaching ${host}:${files}")
				scenario.embed(
						session.executeRemoteCommand("sudo cat -n ${files}").getBytes("UTF-16"),
						"text/plain"
				)
				session.close()

			} catch (RemoteException re) {
				scenario.write("ERROR: problem attaching $files - ${re.message}")
			}

		} catch (RuntimeSshException sshe) {
			scenario.write("could not get $files from $host. host is not available: ${sshe}")
		}
	}
	client.stop()
}
