package com.github.kerubistan.kerub.it.blocks.virt

import com.github.kerubistan.kerub.it.sizes.Sizes
import com.github.kerubistan.kerub.it.utils.Environment
import com.github.kerubistan.kerub.it.utils.SshUtil
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.When
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.junit.Assert
import org.libvirt.Connect
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

class VirtDefs {

	static String home = "/home/${Environment.getStorageUser()}/"

	static disks = [
			"centos_7" : new Tuple("kerub-centos-7-all-5.qcow2", 9),
			"opensuse_42": new Tuple("kerub-openSUSE-42-all-3.qcow2", 13),
			"freebsd_11": new Tuple("kerub-freebsd-11-all-1.qcow2", 1),
			"freebsd_12": new Tuple("kerub-freebsd-12-all-1.qcow2", 1)
	]

	Map<String, UUID> vms = new HashMap()
	Map<String, UUID> vnets = new HashMap()
	List<String> vmDisks = []
	Connect connect = null

	final static Logger logger = LoggerFactory.getLogger(VirtDefs)

	Scenario scenario


	@Before
	void setup(Scenario scenario) {
		this.scenario = scenario
		logger.info("--- virt setup ---")
		def hypervisorUrl = Environment.getHypervisorUrl()
		logger.info("connecting $hypervisorUrl")
		connect = new Connect(hypervisorUrl)
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
		""".stripMargin()
		logger.info("network xml:\n {}", networkXml)
		scenario.write(networkXml.replaceAll("<","&lt;").replaceAll(">","&gt;"))
		vnets.put(name, id)
		connect.listNetworks().each {
			if(it == name) {
				scenario.println("oops- there is a left-behind network " + name + " destroying it...")
				connect.networkLookupByName(name).destroy()
				scenario.println("network " + name + " destroyed")
			}
		}
		scenario.println("defining the new network")
		connect.networkCreateXML(networkXml)
		scenario.println("done")
	}

	@Given("virtual disks")
	void createVirtualDisk(DataTable details) {
		def session = createSshSession()

		def list = details.raw()
		for(def row in list.subList(1, list.size())) {
			def diskName = row.get(0)
			def diskSize = Sizes.toSize(row.get(1))
			session.executeRemoteCommand("rm -f $diskName")
			session.executeRemoteCommand("truncate -s $diskSize $diskName && chmod 777 $diskName")
			vmDisks.add(diskName)
		}
	}

	@Given("^virtual machine (\\S+)")
	void createVirtualMachine(String name, DataTable details) {
		logger.info("create vm $name")
		def id = UUID.randomUUID()
		def params = details.asMap(String, String)
		def disk = disks[params['disk']]

		GString vmDisk = createVmDisk(id, disk)

		def busCntr = 9
		def extraDisks = ""
		for(def key: params.keySet().findAll { it.startsWith("extra-disk:") }) {
			def target = key.replaceAll("extra-disk:","")
			extraDisks += """
				<disk type='file' device='disk'>
					<driver name='qemu' type='raw'/>
					<source file='$home/${params[key]}'/>
					<target dev='$target' bus='virtio'/>
					<address type='pci' domain='0x0000' bus='0x00' slot='0x${Integer.toHexString(busCntr++)}' function='0x0'/>
				</disk>
			""".stripMargin()
		}

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
		scenario.write(domainXml.replaceAll("<","&lt;").replaceAll(">","&gt;"))
		logger.info("vm definition: {}", domainXml)
		connect.nodeInfo().sockets
		connect.domainCreateXML(domainXml, 0)
		vms.put(name, id)
	}

	private GString createVmDisk(UUID id, Tuple disk) {
		def session = createSshSession()

		def templateImg = "$home/${disk.get(0).toString()}"

		session.executeRemoteCommand("qemu-img create -f qcow2 -o backing_file=$templateImg $home/${id}.qcow2")
		session.executeRemoteCommand("chmod 777 $home/${id}.qcow2 ")

		session.close()
		vmDisks.add("$home/${id}.qcow2")
		"${id}.qcow2"
	}

	private ClientSession createSshSession() {
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

	@Given("we wait until (\\S+) comes online, timeout: (\\d+) seconds")
	void waitUntilPing(String address, long timeout) {
		def client = SshUtil.createSshClient()
		def start = System.currentTimeMillis()
		def end = start + (timeout * 1000)
		while(System.currentTimeMillis() < end) {
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



	@When("server (\\S+) crashes")
	void destroyVm(String name) {
		connect.domainLookupByUUID(vms[name]).destroy()
	}

	@When("server (\\S+) shuts down")
	void shutDownVm(String name) {
		connect.domainLookupByUUID(vms[name]).shutdown()
	}

	@After(order = Integer.MIN_VALUE)
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
		if(connect != null) {
			connect.close()
		}
	}

}
