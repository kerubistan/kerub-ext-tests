package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.blocks.tempdata.TempDefs
import com.github.kerubistan.kerub.it.utils.SshUtil
import com.github.kerubistan.kerub.it.utils.TemplateUtil
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import org.apache.sshd.common.scp.ScpTimestamp

import java.nio.charset.Charset
import java.nio.file.attribute.PosixFilePermission

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
		if (!error.isEmpty()) {
			scenario.write("err: $error")
		}


		session.close()
		client.close()
	}

	@Given("file on (\\S+): (\\S+) generated from (\\S+) using parameters")
	void writeToTemplate(String nodeAddress, String path, String templatePath, DataTable dataTable) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		Configuration cfg = new Configuration()
		cfg.templateLoader = new ClassTemplateLoader()
		Template template = cfg.getTemplate(templatePath)
		def writer = new StringWriter()
		template.process(TemplateUtil.convert(dataTable), writer)

		session.createScpClient().upload(
				writer.toString().getBytes(),
				path,
				Arrays.asList(
						PosixFilePermission.GROUP_READ,
						PosixFilePermission.OWNER_WRITE
				),
				new ScpTimestamp(System.currentTimeMillis(), System.currentTimeMillis())
		)

		session.close()
		client.close()

	}

	@Given("Temporary (\\S+) can be appended to (\\S+) on (\\S+)")
	void appendTempToFile(String tempName, String path, String nodeAddress) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def command = "sudo mkdir -p ${path.substring(0, path.lastIndexOf("/"))}"
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))

		def tempValue = TempDefs.instance.get().getData(tempName).trim()
		command = """sudo bash -c "echo ${tempValue} >> $path" """
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))
		command = "sudo cat $path"
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))
	}
}