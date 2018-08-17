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
import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import org.apache.commons.io.IOUtils
import org.apache.sshd.common.scp.ScpTimestamp

import java.nio.charset.Charset
import java.nio.file.attribute.PosixFilePermission
import java.rmi.RemoteException

import static org.junit.Assert.assertEquals

class ExecDefs {

	Scenario scenario = null

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Given("command template executed on (\\S+): (\\S+) / (.*)")
	void executeTemplateCommandOnNode(String nodeAddress, String imageName, String commandId) {
		String commandTemplate = OsImages.getOsCommand(imageName, commandId)

		Configuration cfg = new Configuration()
		def loader = new StringTemplateLoader()
		loader.putTemplate("template", commandTemplate)
		cfg.templateLoader = loader
		Template template = cfg.getTemplate("template")
		def writer = new StringWriter()
		def params = new HashMap<String, Object>()
		params.putAll(OsImages.getValues(imageName))
		params.put("packageFile", new File("ospackages/$imageName").listFiles()[0].getName())
		template.process(params, writer)
		def command =  writer.toString()

		scenario.write("command executed on $nodeAddress: $command")
		executeOnNode(nodeAddress, command)
	}

	@Given("command executed on (\\S+):(.*)")
	void executeOnNode(String nodeAddress, String command) {

		def start = System.currentTimeMillis()

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def err = new ByteArrayOutputStream()
		def out = new ByteArrayOutputStream()
		try {
			session.executeRemoteCommand(command, out, err, Charset.forName("UTF-16"))
		} catch(RemoteException re) {
			//tolerated, it happens when reboot
			scenario.write("remote exception" + re.message)
		} finally {

			def error = new String(err.toByteArray())
			if (!error.isEmpty()) {
				scenario.write("err: $error")
			}

			def output = new String(out.toByteArray())
			if (!output.isEmpty()) {
				scenario.write("out: $output")
			}

			def end = System.currentTimeMillis()
			scenario.write("command took ${end - start} miliseconds")

		}

		session.close()
		client.close()
	}

	@Given("(\\S+) package file uploaded to (\\S+) directory (\\S+)")
	void uploadFile(String distroName, String nodeAddress, String directory) {

		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def sftpClient = session.createSftpClient()
		def files = new File("ospackages/$distroName").listFiles()
		assertEquals(1, files.length)
		def file = files[0]

		scenario.write("uploading ${file.getAbsolutePath()} to $nodeAddress:$directory")
		def bytes = file.readBytes()
		sftpClient.write("$directory/${file.getName()}").write(bytes)
		def output = sftpClient.write("$directory/${file.getName()}")
		def cnt = IOUtils.copy(new FileInputStream(file), output)
		output.close()
		scenario.write("upload done: $cnt bytes")
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