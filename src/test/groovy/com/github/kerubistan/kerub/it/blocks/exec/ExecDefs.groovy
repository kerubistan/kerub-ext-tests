package com.github.kerubistan.kerub.it.blocks.exec

import com.github.kerubistan.kerub.it.blocks.osimages.OsImages
import com.github.kerubistan.kerub.it.utils.SshUtil
import com.github.kerubistan.kerub.it.utils.TemplateUtil
import cucumber.api.Scenario
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import freemarker.cache.ClassTemplateLoader
import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import io.cucumber.datatable.DataTable
import org.apache.commons.io.IOUtils
import org.apache.sshd.common.SshException
import org.apache.sshd.common.scp.ScpTimestamp

import java.nio.charset.Charset
import java.nio.file.attribute.PosixFilePermission
import java.rmi.RemoteException

import static org.junit.Assert.assertEquals

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

class ExecEnvironment {
}

class Logger {
	String name
	String level
}


World {
	new ExecEnvironment()
}

Given(~/^command template executed on (\S+): (\S+) \/ (\S+)$/) {
	String nodeAddress, String imageName, String commandId ->
		String commandTemplate = OsImages.getOsCommand(imageName, commandId)

		def params = new HashMap<String, Object>()
		params.putAll(OsImages.getValues(imageName))
		params.put("packageFile", new File("ospackages/$imageName").listFiles()[0].getName())

		String command = processTemplate(commandTemplate, params)

		scenario.write("command executed on $nodeAddress: $command")
		executeOnNode(scenario, nodeAddress, command)
}

private static String processTemplate(String commandTemplate, HashMap<String, Object> params) {
	Template template = makeTemplate(commandTemplate)
	def writer = new StringWriter()
	template.process(params, writer)
	def command = writer.toString()
	command
}

private static Template makeTemplate(String commandTemplate) {
	Configuration cfg = new Configuration()
	def loader = new StringTemplateLoader()
	loader.putTemplate("template", commandTemplate)
	cfg.templateLoader = loader
	Template template = cfg.getTemplate("template")
	template
}

Given(~/^command executed on (\S+):(.*)$/) {
	String nodeAddress, String command ->

		scenario.write("command executed on $nodeAddress: $command")
		executeOnNode(scenario, nodeAddress, command)


}

Given(~/^(\S+) package file uploaded to (\S+) directory (\S+)$/) {
	String distroName, String nodeAddress, String directory ->

		def attempt = 0
		def success = false
		while (!success && attempt < 5) {
			attempt++
			try {
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
				scenario.write("upload done: $cnt bytes")
				output.close()
				success = true
			} catch (Exception e) {
				scenario.write(e.toString())
			}
		}
}

Given(~/^file on (\S+): (\S+) generated from (\S+) using parameters$/) {
	String nodeAddress, String path, String templatePath, DataTable dataTable ->

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

Given(~/^Temporary (\S+) can be appended to (\S+) on (\S+)$/) {
	String tempName, String path, String nodeAddress ->
		def client = SshUtil.createSshClient()
		def session = SshUtil.loginWithTestUser(client, nodeAddress)

		def command = "sudo mkdir -p ${path.substring(0, path.lastIndexOf("/"))}"
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))

		def tempValue = getData(tempName).trim()
		command = """sudo bash -c "echo ${tempValue} >> $path" """
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))
		command = "sudo cat $path"
		scenario.write(command)
		scenario.write(session.executeRemoteCommand(command))
}

And(~/^kerub logger update on (\S+), root is (\S+) level$/) { String addr, String rootLevel, DataTable levels ->
	def client = SshUtil.createSshClient()
	def session = SshUtil.loginWithTestUser(client, addr)

	def params = new HashMap<String, Object>()
	params.put("rootLevel", rootLevel)
	def loggers = new ArrayList<Logger>()
	params.put("loggers", loggers)
	for (row in levels.cells()) {
		def logger = new Logger()
		logger.name = row[0]
		logger.level = row[1]
		loggers.add(logger)
	}
	def loggerConfig = processTemplate(
			Thread.currentThread().getContextClassLoader().getResourceAsStream("logback-template.xml.fm").text,
			params
	)

	def success = false
	def cntr = 0
	while (!success && cntr < 10) {
		cntr++
		try {
			def sftpClient = session.createSftpClient()
			def output = sftpClient.write("/tmp/kerub-logback.xml")
			def writer = output.newWriter()
			writer.write(loggerConfig)
			writer.close()
			success = true
		} catch (SshException sshe) {
			scenario.write("no luck " + sshe.message)
		}
	}

	session.executeRemoteCommand("sudo cp -f /tmp/kerub-logback.xml /etc/kerub/logback.xml")

	executeOnNode(scenario, addr, "sudo cat /etc/kerub/logback.xml")
}

And(~/^we fetch basic linux host info from (\S+)$/) {
	String hostAddr ->
		executeOnNode(scenario, hostAddr, "cat -n /proc/cpuinfo")
		executeOnNode(scenario, hostAddr, "cat -n /proc/meminfo")
		executeOnNode(scenario, hostAddr, "df -h")
		executeOnNode(scenario, hostAddr, "lsblk")
		executeOnNode(scenario, hostAddr, "uname -a")
		executeOnNode(scenario, hostAddr, "lspci")
}

private static void executeOnNode(Scenario scenario, String nodeAddress, String command) {
	def start = System.currentTimeMillis()

	def client = SshUtil.createSshClient()
	def session = SshUtil.loginWithTestUser(client, nodeAddress)

	def err = new ByteArrayOutputStream()
	def out = new ByteArrayOutputStream()
	scenario.write("remote command on $nodeAddress: $command")
	try {
		session.executeRemoteCommand(command, out, err, Charset.forName("UTF-16"))
	} catch (RemoteException re) {
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

And(~/^we fetch basic bsd host info from (\S+)$/) {
	String hostAddr ->
		executeOnNode(scenario, hostAddr, "uname -a")
		executeOnNode(scenario, hostAddr, "free -m")
		executeOnNode(scenario, hostAddr, "df -h")
		executeOnNode(scenario, hostAddr, "geom drive list")
		executeOnNode(scenario, hostAddr, "lspci")
}
