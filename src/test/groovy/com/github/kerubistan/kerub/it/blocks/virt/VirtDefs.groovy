package com.github.kerubistan.kerub.it.blocks.virt

import com.github.kerubistan.kerub.it.sizes.Sizes
import com.github.kerubistan.kerub.it.utils.Environment
import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import io.cucumber.datatable.DataTable
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.junit.Assert
import org.libvirt.Connect
import org.libvirt.LibvirtException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

class VirtEnvironment {
	static String home = "/home/${Environment.getStorageUser()}/"

	static disks = [
			"centos_6": new Tuple("kerub-centos-6-host-1.qcow2", 1),
			"centos_7" : new Tuple("kerub-centos-7-all-5.qcow2", 9),
			"debian_9_arm" : new Tuple("kerub-debian-9-arm-1.qcow2", 1),
			"opensuse_42": new Tuple("kerub-openSUSE-42-all-3.qcow2", 13),
			"ubuntu_16": new Tuple("kerub-ubuntu-16-all-1.qcow2", 1),
			"ubuntu_18": new Tuple("kerub-ubuntu-18-all-1.qcow2", 1),
			"ubuntu_18_arm": new Tuple("kerub-ubuntu-18-arm-1.qcow2", 1),
			"freebsd_11": new Tuple("kerub-freebsd-11-all-1.qcow2", 1),
			"freebsd_12": new Tuple("kerub-freebsd-12-all-1.qcow2", 1)
	]

	Map<String, UUID> vms = new HashMap()
	Map<String, UUID> vnets = new HashMap()
	List<String> vmDisks = []
	Connect connect = null

	final static Logger logger = LoggerFactory.getLogger(VirtDefs)

	GString createVmDisk(UUID id, Tuple disk) {
		def session = createSshSession()

		def templateImg = "$home/${disk.get(0).toString()}"

		session.executeRemoteCommand("qemu-img create -f qcow2 -o backing_file=$templateImg $home/${id}.qcow2")
		session.executeRemoteCommand("chmod 777 $home/${id}.qcow2 ")

		session.close()
		vmDisks.add("$home/${id}.qcow2")
		"${id}.qcow2"
	}

	GString createVmFd(UUID id, Tuple disk) {
		def session = createSshSession()

		def templateImg = "$home/${disk.get(0).toString()}".replaceAll("qcow2", "fd")

		session.executeRemoteCommand("cp $templateImg $home/${id}.fd")
		session.executeRemoteCommand("chmod 777 $home/${id}.fd ")

		session.close()
		vmDisks.add("$home/${id}.fd")
		"${id}.fd"
	}


	static ClientSession createSshSession() {
		def ssh = SshClient.setUpDefaultClient()
		ssh.start()
		def connectFuture = ssh.connect(Environment.getStorageUser(), Environment.getStorageHost(), 22)
		connectFuture.await()
		def session = connectFuture.getSession()
		session.addPasswordIdentity(Environment.getStoragePassword())
		def authFuture = session.auth()
		authFuture.await(1, TimeUnit.SECONDS)
		authFuture.verify()
		session
	}


}

World {
	new VirtEnvironment()
}

Before {
	logger.info("--- virt setup ---")
	def hypervisorUrl = Environment.getHypervisorUrl()
	logger.info("connecting $hypervisorUrl")
	connect = new Connect(hypervisorUrl)
	logger.info("connected: {}", connect.connected)
}

Given(~"^virtual network (\\S+) domain name (\\S+)") {
	String name, String domain, DataTable nics ->
		def id = UUID.randomUUID()
		def builder = new StringBuilder()
		nics.cells().subList(1, nics.cells().size()).each {
			builder.append("<host mac='${it[1]}' name='${it[0]}' ip='${it[2]}'/>\n")
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
		""".stripMargin()
		logger.info("network xml:\n {}", networkXml)
		scenario.write(networkXml.replaceAll("<", "&lt;").replaceAll(">", "&gt;"))
		vnets.put(name, id)
		def _scenario = scenario
		def _connect = connect
		connect.listNetworks().each { networkName ->
			if (networkName == name) {
				_scenario.println("oops- there is a left-behind network " + name + " destroying it...")
				_connect.networkLookupByName(name).destroy()
				_scenario.println("network " + name + " destroyed")
			}
		}
		scenario.println("defining the new network")
		connect.networkCreateXML(networkXml)
		scenario.println("done")
}

Given(~"virtual disks") { DataTable details ->
	def session = createSshSession()

	def list = details.cells()
	for (def row in list.subList(1, list.size())) {
		def diskName = row[0]
		def diskSize = Sizes.toSize(row[1])
		session.executeRemoteCommand("rm -f $diskName")
		session.executeRemoteCommand("truncate -s $diskSize $diskName && chmod 777 $diskName")
		vmDisks.add(diskName)
	}
}

Given(~"^ARM64 virtual machine (\\S+)") { String name, DataTable details ->
	logger.info("create vm $name")
	try {
		def old = connect.domainLookupByName(name)
		if (old != null) {
			logger.info("trying to destroy left-behind domain " + name)
			old.destroy()
		}
	} catch (LibvirtException e) {
		//this is fine
	}
	def id = UUID.randomUUID()
	def params = details.asMap(String, String)
	def disk = disks[params['disk']]

	GString vmDisk = createVmDisk(id, disk)
	GString vmFd = createVmFd(id, disk)


	String extraDisks = extraDisksXml(params, home)

	def domainXml = """
<domain type='qemu'>
  <name>$name</name>
  <uuid>$id</uuid>
  <memory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</memory>
  <currentMemory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</currentMemory>
  <vcpu placement='static'>${params['cpus'] ?: 1}</vcpu>
  <os>
    <type arch='aarch64' machine='virt-2.11'>hvm</type>
    <loader readonly='yes' type='pflash'>/usr/share/AAVMF/AAVMF_CODE.fd</loader>
    <nvram>$home/t$vmFd</nvram>
  </os>
  <features>
    <acpi/>
    <gic version='2'/>
  </features>
  <cpu mode='custom' match='exact' check='none'>
    <model fallback='allow'>cortex-a57</model>
  </cpu>
  <clock offset='utc'/>
  <on_poweroff>destroy</on_poweroff>
  <on_reboot>restart</on_reboot>
  <on_crash>destroy</on_crash>
  <devices>
    <emulator>/usr/bin/qemu-system-aarch64</emulator>
	<disk type='file' device='disk'>
	  <driver name='qemu' type='qcow2'/>
	  <source file='$home/$vmDisk'/>
	  <target dev='sda' bus='scsi'/>
	  <address type='drive' controller='0' bus='0' target='0' unit='0'/>
	  <boot order='1'/>
	</disk>
	$extraDisks
    <controller type='usb' index='0' model='qemu-xhci' ports='8'>
      <address type='pci' domain='0x0000' bus='0x02' slot='0x00' function='0x0'/>
    </controller>
    <controller type='scsi' index='0' model='virtio-scsi'>
      <address type='pci' domain='0x0000' bus='0x03' slot='0x00' function='0x0'/>
    </controller>
    <controller type='pci' index='0' model='pcie-root'/>
    <controller type='pci' index='1' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='1' port='0x8'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x0' multifunction='on'/>
    </controller>
    <controller type='pci' index='2' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='2' port='0x9'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>
    </controller>
    <controller type='pci' index='3' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='3' port='0xa'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>
    </controller>
    <controller type='pci' index='4' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='4' port='0xb'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x3'/>
    </controller>
    <controller type='pci' index='5' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='5' port='0xc'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x4'/>
    </controller>
    <controller type='pci' index='6' model='pcie-root-port'>
      <model name='pcie-root-port'/>
      <target chassis='6' port='0xd'/>
      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x5'/>
    </controller>
    <controller type='virtio-serial' index='0'>
      <address type='pci' domain='0x0000' bus='0x04' slot='0x00' function='0x0'/>
    </controller>
    <interface type='network'>
	  <mac address='${params['mac']}'/>
	  <source network='${params['net'] ?: 'default'}'/>
      <model type='virtio'/>
      <address type='pci' domain='0x0000' bus='0x01' slot='0x00' function='0x0'/>
    </interface>
    <serial type='pty'>
      <target type='system-serial' port='0'>
        <model name='pl011'/>
      </target>
    </serial>
    <console type='pty'>
      <target type='serial' port='0'/>
    </console>
    <channel type='unix'>
      <target type='virtio' name='org.qemu.guest_agent.0'/>
      <address type='virtio-serial' controller='0' bus='0' port='1'/>
    </channel>
    <rng model='virtio'>
      <backend model='random'>/dev/urandom</backend>
      <address type='pci' domain='0x0000' bus='0x05' slot='0x00' function='0x0'/>
    </rng>
  </devices>
</domain>

""".stripMargin()
	scenario.write(domainXml.replaceAll("<", "&lt;").replaceAll(">", "&gt;"))
	logger.info("vm definition: {}", domainXml)
	connect.nodeInfo().sockets
	connect.domainCreateXML(domainXml, 0)
	vms.put(name, id)

}

Given(~"^virtual machine (\\S+)") { String name, DataTable details ->
	logger.info("create vm $name")
	try {
		def old = connect.domainLookupByName(name)
		if (old != null) {
			logger.info("trying to destroy left-behind domain " + name)
			old.destroy()
		}
	} catch (LibvirtException e) {
		//this is fine
	}
	def id = UUID.randomUUID()
	def params = details.asMap(String, String)
	def disk = disks[params['disk']]

	GString vmDisk = createVmDisk(id, disk)

	String extraDisks = extraDisksXml(params, home)

	def domainXml = """
			<domain type='kvm'>
			  <name>$name</name>
			  <uuid>$id</uuid>
			  <memory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</memory>
			  <currentMemory unit='${params['ram'].split(' ')[1]}'>${params['ram'].split(' ')[0]}</currentMemory>
			  <vcpu placement='static'>${params['cpus'] ?: 1}</vcpu>
			  <os>
				<type arch='x86_64'>hvm</type>
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
				<disk type='file' device='disk'>
				  <driver name='qemu' type='qcow2'/>
				  <source file='$home/$vmDisk'/>
				  <target dev='vda' bus='virtio'/>
				  <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>
				</disk>
				$extraDisks
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
				<rng model='virtio'>
				  <backend model='random'>/dev/random</backend>
				  <address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>
				</rng>
			  </devices>
			</domain>
		""".stripMargin()
	scenario.write(domainXml.replaceAll("<", "&lt;").replaceAll(">", "&gt;"))
	logger.info("vm definition: {}", domainXml)
	connect.nodeInfo().sockets
	connect.domainCreateXML(domainXml, 0)
	vms.put(name, id)
}

private String extraDisksXml(params, home) {
	def busCntr = 9
	def extraDisks = ""
	for (def key : params.keySet().findAll { it.startsWith("extra-disk:") }) {
		def target = key.replaceAll("extra-disk:", "")
		extraDisks += """
				<disk type='file' device='disk'>
					<driver name='qemu' type='raw'/>
					<source file='$home/${params[key]}'/>
					<target dev='$target' bus='virtio'/>
					<address type='pci' domain='0x0000' bus='0x00' slot='0x${Integer.toHexString(busCntr++)}' function='0x0'/>
				</disk>
			""".stripMargin()
	}
	extraDisks
}


Given(~/^we wait until (\S+) comes online with timeout: (\d+) seconds$/) {
	String address, int timeout ->
		def client = SshUtil.createSshClient()
		def start = System.currentTimeMillis()
		def end = start + (timeout * 1000)
		while (System.currentTimeMillis() < end) {
			try {
				SshUtil.loginWithTestUser(client, address)
				scenario.write("connected: $address")
				return
			} catch (Exception e) {
				Thread.sleep(1000)
				scenario.write("$address not connected ${System.currentTimeMillis()} - waiting")
				logger.info("still waiting for $address (${end - System.currentTimeMillis()} ms left)")
				//fine, wait until it wakes up
			}
		}
		Assert.fail("node $address is still not available")
}


When(~"server (\\S+) crashes") { String name ->
	connect.domainLookupByUUID(vms[name]).destroy()
}

When(~"server (\\S+) shuts down") { String name ->
	connect.domainLookupByUUID(vms[name]).shutdown()
}

After(order = Integer.MIN_VALUE) {
	logger.info("--- virt cleanups ---")
	def _logger = logger
	def _connect = connect
	vms.entrySet().forEach {
		_logger.info("stop and remove ${it.key}")
		try {
			_connect.domainLookupByUUID(it.value).destroy()
		} catch (LibvirtException e) {
			_logger.info("could not shut down ${it.key} / ${it.value}", e)
		}
	}
	def _session = createSshSession()
	vmDisks.forEach {
		_session.executeRemoteCommand("rm -rf $it")
	}
	_session.close()
	vnets.entrySet().forEach {
		_logger.info("destroying network ${it.key}")
		try {
			_connect.networkLookupByUUID(it.value).destroy()
		} catch (LibvirtException e) {
			_logger.info("could not shut down network ${it.key} / ${it.value}", e)
		}
	}
	if (connect != null) {
		connect.close()
		try {
			connect.close()
		} catch (LibvirtException e) {
			logger.info("could not disconnect from libvirt", e)
		}
	}
}

