package com.github.kerubistan.kerub.it.blocks.virt

import cucumber.api.DataTable
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.When
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.scp.ScpTimestamp
import org.junit.Assert
import org.libvirt.Connect
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.TimeUnit

class VirtDefs {

	static disks = [
			"centos_7" : "kerub-centos-7.tar.bz2",
			"ubuntubsd": "kerub-ubuntubsd.tar.bz2"
	]

	Map<String, UUID> vms = new HashMap()
	Map<String, UUID> vnets = new HashMap()
	List<String> vmDisks = []
	Connect connect = null

	final static Logger logger = LoggerFactory.getLogger(VirtDefs)

	@Before
	void setup() {
		logger.info("--- virt setup ---")
		connect = new Connect("qemu:///system")
		logger.info("connected: {}", connect.connected)
	}

	@Given("^virtual network (\\S+) domain name (\\S+)")
	void createVirtualNetwork(String name, String domain, DataTable nics) {
		def id = UUID.randomUUID()
		def builder = new StringBuilder()
		nics.asList(DhcpClientConfig).each {
			builder.append("<host mac='${it.getMac()}' name='${it.getHost()}' ip='${it.getIp()}'/>\n")
		}

		def networkXml = """
<network>
<name>$name</name>
<uuid>$id</uuid>
<forward mode='nat'/>
<mac address='52:54:00:b2:79:76'/>
<domain name='$domain'/>
<ip address='192.168.123.1' netmask='255.255.255.0'>
<dhcp>
<range start='192.168.123.2' end='192.168.123.254'/>
${builder}
</dhcp>
</ip>
</network>
"""
		logger.info("network xml:\n {}", networkXml)
		vnets.put(name, id)
		connect.networkCreateXML(networkXml)
	}

	@Given("^virtual machine (\\S+)")
	void createVirtualMachine(String name, DataTable details) {
		logger.info("create vm $name")
		def id = UUID.randomUUID()
		def params = details.asMap(String, String)
		def disk = disks[params['disk']]

		GString vmDisk = createVmDisk(id, disk)

		def domainXml = """
<domain type='kvm'>
  <name>$name</name>
  <uuid>$id</uuid>
  <memory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</memory>
  <currentMemory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</currentMemory>
  <vcpu placement='static'>${params['cpus'] ?: 1}</vcpu>
  <os>
    <type arch='x86_64' machine='pc-i440fx-xenial'>hvm</type>
    <boot dev='hd'/>
  </os>
  <features>
    <acpi/>
    <apic/>
  </features>
  <cpu mode='host-passthrough'/>
  <clock offset='utc'>
    <timer name='rtc' tickpolicy='catchup'/>
    <timer name='pit' tickpolicy='delay'/>
    <timer name='hpet' present='no'/>
  </clock>
  <on_poweroff>destroy</on_poweroff>
  <on_reboot>restart</on_reboot>
  <on_crash>restart</on_crash>
  <pm>
    <suspend-to-mem enabled='no'/>
    <suspend-to-disk enabled='no'/>
  </pm>
  <devices>
    <emulator>/usr/bin/kvm-spice</emulator>
    <disk type='file' device='disk'>
      <driver name='qemu' type='qcow2'/>
      <source file='$vmDisk'/>
      <target dev='vda' bus='virtio'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>
    </disk>
    <controller type='usb' index='0' model='ich9-ehci1'>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x7'/>
    </controller>
    <controller type='usb' index='0' model='ich9-uhci1'>
      <master startport='0'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0' multifunction='on'/>
    </controller>
    <controller type='usb' index='0' model='ich9-uhci2'>
      <master startport='2'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x1'/>
    </controller>
    <controller type='usb' index='0' model='ich9-uhci3'>
      <master startport='4'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x2'/>
    </controller>
    <controller type='pci' index='0' model='pci-root'/>
    <controller type='virtio-serial' index='0'>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>
    </controller>
    <interface type='network'>
      <mac address='${params['mac']}'/>
      <source network='${params['net'] ?: 'default'}'/>
      <model type='virtio'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>
    </interface>
    <serial type='pty'>
      <target port='0'/>
    </serial>
    <console type='pty'>
      <target type='serial' port='0'/>
    </console>
    <channel type='unix'>
      <source mode='bind'/>
      <target type='virtio' name='org.qemu.guest_agent.0'/>
      <address type='virtio-serial' controller='0' bus='0' port='1'/>
    </channel>
    <channel type='spicevmc'>
      <target type='virtio' name='com.redhat.spice.0'/>
      <address type='virtio-serial' controller='0' bus='0' port='2'/>
    </channel>
    <input type='tablet' bus='usb'/>
    <input type='mouse' bus='ps2'/>
    <input type='keyboard' bus='ps2'/>
    <graphics type='spice' autoport='yes'>
      <image compression='off'/>
    </graphics>
    <video>
      <model type='qxl' ram='65536' vram='65536' vgamem='16384' heads='1'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>
    </video>
    <redirdev bus='usb' type='spicevmc'>
    </redirdev>
    <redirdev bus='usb' type='spicevmc'>
    </redirdev>
  </devices>
</domain>

"""
		logger.info("vm definition: {}", domainXml)
		connect.domainCreateXML(domainXml, 0)
		vms.put(name, id)
	}

	private GString createVmDisk(UUID id, String disk) {
		def session = createSshSession()
		def sftpClient = session.createSftpClient()
		sftpClient.mkdir("/var/lib/libvirt/images/$id")
		def scpClient = session.createScpClient()
		logger.info("authenticated: {}, open: {}", session.isAuthenticated(), session.isOpen())
		scpClient.upload(
				Thread.currentThread().contextClassLoader.getResourceAsStream(disk),
				"/var/lib/libvirt/images/$id/$disk",
				size(disk),
				[PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE],
				new ScpTimestamp(System.currentTimeMillis(), System.currentTimeMillis())
		)

		def tarResult = session.executeRemoteCommand("tar -C /var/lib/libvirt/images/$id -xjvSf /var/lib/libvirt/images/$id/$disk")
		logger.info("tar: {}", tarResult)
		session.executeRemoteCommand("rm -f /var/lib/libvirt/images/$id/$disk")
		def vmDisk = "/var/lib/libvirt/images/$id/${tarResult.trim()}"

		session.close()
		vmDisks.add("/var/lib/libvirt/images/$id/")
		vmDisk
	}

	long size(String resource) {
		def input = Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
		def size = 0
		while (input.read() != -1) {
			size++
		}
		size
	}

	private ClientSession createSshSession() {
		def ssh = SshClient.setUpDefaultClient()
		ssh.start()
		def connectFuture = ssh.connect("kerub-it-test", "localhost", 22)
		connectFuture.await(/*1, TimeUnit.SECONDS*/)
		def session = connectFuture.getSession()
		session.addPasswordIdentity("password")
		def authFuture = session.auth()
		authFuture.await(1, TimeUnit.SECONDS)
		authFuture.verify()
		session
	}

	@Given("we wait until (\\S+) comes online, timeout: (\\d+) seconds")
	void waitUntilPing(String address, long timeout) {
		def start = System.currentTimeMillis()
		def end = start + (timeout * 1000)
		while(System.currentTimeMillis() < end) {
			try {
				InetAddress.getByName(address)
				return
			} catch (UnknownHostException uhe) {
				Thread.sleep(1000)
				logger.info("still waiting for $address (${end - System.currentTimeMillis()} ms left)")
				//fine, wait until it wakes up
			}
		}
		Assert.fail("node $address is still not available")
	}

	@When("server (\\S+) crashes")
	void destroyVm(String name) {
		connect.domainLookupByUUID(vms[name]).destroy()
	}

	@When("server (\\S+) shuts down")
	void shutDownVm(String name) {
		connect.domainLookupByUUID(vms[name]).shutdown()
	}

	@After
	void cleanup() {
		logger.info("--- virt cleanups ---")
		vms.entrySet().forEach {
			logger.info("stop and remove ${it.key}")
			connect.domainLookupByUUID(it.value).destroy()
		}
		vmDisks.forEach {
			def session = createSshSession()
			session.executeRemoteCommand("rm -rf $it")
			session.close()
		}
		vnets.entrySet().forEach {
			logger.info("destroying network ${it.key}")
			connect.networkLookupByUUID(it.value).destroy()
		}
		connect.close()
	}

}
