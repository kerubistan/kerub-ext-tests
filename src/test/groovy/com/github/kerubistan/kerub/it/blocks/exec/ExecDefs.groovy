package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.blocks.osimages.OsImages
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

	@Given("command template executed on (\\S+): (\\S+) / (.*)")
	void executeTemplateCommandOnNode(String nodeAddress, String imageName, String commandId) {
		String command = OsImages.getOsCommand(imageName, commandId)
		scenario.write("command executed on $nodeAddress: $command")
		executeOnNode(nodeAddress, command)
	}

	@Given("command executed on (\\S+):(.*)")
	void executeOnNode(String nodeAddress, String command) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def err = new ByteArrayOutputStream()
		try {
			def out = session.executeRemoteCommand(command, err, Charset.forName("ASCII"))
			scenario.write(out)
		} finally {
			def error = new String(err.toByteArray())
			if (!error.isEmpty()) {
				scenario.write("err: $error")
			}
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