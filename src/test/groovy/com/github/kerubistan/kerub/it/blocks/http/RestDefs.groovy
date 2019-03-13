package com.github.kerubistan.kerub.it.blocks.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.github.kerubistan.kerub.it.blocks.tempdata.TempDefs
import com.github.kerubistan.kerub.it.sizes.Sizes
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.And
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import gherkin.formatter.model.DataTableRow
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.commons.io.output.NullOutputStream
import org.apache.http.HttpResponse
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.junit.Assert

import java.util.zip.GZIPInputStream

import static com.github.kerubistan.kerub.it.utils.PropertyUtil.toMap

class RestDefs {
	Scenario scenario = null

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Then("session (\\S+): user can download kerub controller public ssh key to temp (\\S+)")
	void downloadAndSetSshKey(String sessionId, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def response = client.execute(HttpDefs.instance.get().get("/s/r/host/helpers/controller-pubkey"))
		assertResponseStatus(response, 200)
		def text = response.entity.content.getText()
		scenario.write("controller public key:\n $text")
		TempDefs.instance.get().setData(tempName, text)
	}

	@Then("session (\\S+): user can fetch public key for (\\S+) into temp (\\S+)")
	void getPublicKey(String sessionId, String address, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def response = client.execute(HttpDefs.instance.get().get("/s/r/host/helpers/pubkey?address=$address"))

		assertResponseStatus(response, 200)
		def text = response.entity.content.getText()
		scenario.write("public key: $text")
		def tree = new ObjectMapper().readTree(text)
		def pkey = tree.get("fingerprint").toString()

		TempDefs.instance.get().setData(tempName, pkey)
	}

	@Given("session (\\S+): within (\\d+) seconds the host details will be extended with the discovered details:")
	void checkHostDetails(String sessionId, Integer timeoutSecs, DataTable validations) {
		def client = Clients.instance.get().getClient(sessionId)

		def start = System.currentTimeMillis()

		def tree = null
		while(
				System.currentTimeMillis() - start < timeoutSecs * 1000
						&& (tree == null || tree.get("result").any { it.get("capabilities") == null })) {
			def response = client.execute(HttpDefs.instance.get().get("/s/r/host"))
			assertResponseStatus(response, 200)
			tree = new ObjectMapper().readTree(response.entity.content)
			response.entity.content.close()
		}

		scenario.write("hosts response: ")
		scenario.write(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(tree))
		//TODO: check all requirements in capabilities

		if(System.currentTimeMillis() - start >= timeoutSecs * 1000) {
			throw new Exception("did not make it within $timeoutSecs seconds, probably never will")
		}

	}

	@Then("session (\\S+): user can join host (\\S+) using public key and fingerprint (\\S+) and store ID in temp (\\S+)")
	void joinHostWithPublicKey(String sessionId, String address, String fingerprint, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def hostId = UUID.randomUUID()
		def response = client.execute(HttpDefs.instance.get()
				.put(
				"/s/r/host/join-pubkey",
				"""
				{ 
					"host": {
						"@type": "host",
						"id": "${hostId}",
						"address":"$address",
						"publicKey" : ${TempDefs.instance.get().getData(fingerprint)},
						"dedicated" : true
					},
					"powerManagement" : []
				}""".stripMargin()
				)
		)

		def responseText = response.entity.content.getText("ASCII")
		scenario.write("status:" + response.getStatusLine())
		scenario.write("response:" + responseText)
		assertResponseStatus(response, 200)
		def host = new ObjectMapper().readTree(responseText)

		TempDefs.instance.get().setData(tempName, hostId.toString())
	}

	@Then("session (\\S+): host identified by key (\\S+) should have (\\S+) storage capability registered with size around (\\S+) \\+\\-(\\S+)")
	void verifyStorageCapability(
			String sessionId,
			String hostKey,
			String storageType,
			String sizeEstimate,
			String sizePrecision,
			DataTable props) {
		def capabilities = null
		def attempt = 1
		while(capabilities == null && attempt < 10) {
			def hostId = TempDefs.instance.get().getData(hostKey)
			def client = Clients.instance.get().getClient(sessionId)
			scenario.write("checking /s/r/host/$hostId")
			def response = client.execute(HttpDefs.instance.get().get("/s/r/host/$hostId"))
			def responseText = response.entity.content.getText("ASCII")
			scenario.write("response: ${response.getStatusLine().statusCode}\n $responseText")

			def host = new ObjectMapper().readTree(responseText)
			capabilities = host.get("capabilities")
		}
		Assert.assertNotNull(capabilities)
		def storageCapabilities = capabilities.get("storageCapabilities")
		Assert.assertNotNull(storageCapabilities)

		def storage = storageCapabilities.find {
			def rows = props.raw().subList(1, props.raw().size())
			((it["@type"] as TextNode).textValue() == storageType) && matches(rows, it)
		}
		Assert.assertNotNull(storage)
		Assert.assertEquals(
				Sizes.toSize(sizeEstimate).toDouble(),
				(storage["size"] as LongNode).longValue().toDouble(),
				Sizes.toSize(sizePrecision).toDouble(),
		)
	}

	static boolean matches(List<List<String>> expected, JsonNode actual) {
		for(row in expected) {
			if((actual[row[0]] as TextNode).textValue() != row[1].trim()) {
				return false
			}
		}
		return true
	}

	@And("^session (\\S+): user can upload a (ro|rw)?\\s*(\\S+) file (\\S+) - generated id into into temp:(\\S+)")
	void uploadAFile(String sessionId, String readOnlyStr, String format, String fileName, String tempName) throws Throwable {
		def client = Clients.instance.get().getClient(sessionId)
		def id = UUID.randomUUID()
		def readOnly = "ro" == readOnlyStr

		def size = IOUtils.copy(new GZIPInputStream(
				Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName + ".gz")
		), new NullOutputStream())
		def put = HttpDefs.instance.get().put("s/r/virtual-storage", """
		{
			"@type":"virtual-storage",
			"id" : "$id",
			"name" : "$fileName",
			"size" : "$size",
			"readOnly" : $readOnly
		}
		""".stripMargin())
		def putResponse = client.execute(put)
		logResponse(putResponse)
		assertResponseStatus(putResponse, 200)

		def post = HttpDefs.instance.get().post("s/r/virtual-storage/load/$format/$id")
		// TODO: how the FUCK do I form upload and multipart with this library?
		post.setHeader("Content-Type", "multipart/form-data")
		def countingInputStream = new CountingInputStream(new GZIPInputStream(
				Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName + ".gz")
		))
		post.setEntity(MultipartEntityBuilder.create().addBinaryBody("file", countingInputStream).build())

		def start = System.currentTimeMillis()
		def response = client.execute(post)

		scenario.write("Upload of ${countingInputStream.byteCount} bytes took ${start - System.currentTimeMillis()} ms")
		scenario.write("status code is ${response.getStatusLine().statusCode}")

		Assert.assertEquals(204, response.getStatusLine().statusCode)
		TempDefs.instance.get().setData(tempName, id.toString())
	}

	@And("^session (\\S+): user can create a disk with size (\\S+) - generated id into into temp:(\\S+)")
	void createDisk(String sessionId, String sizeSpec, String tempName) {
		createDiskWithExpectation(sessionId, sizeSpec, tempName, DataTable.create(Collections.emptyList()))
	}

	@And("^session (\\S+): user can create a disk with size (\\S+) - generated id into into temp (\\S+) with expectations")
	void createDiskWithExpectation(String sessionId, String sizeSpec, String tempName, DataTable expectations) {
		def client = Clients.instance.get().getClient(sessionId)
		def id = UUID.randomUUID()

		def put = HttpDefs.instance.get().put("s/r/virtual-storage", """
		{
			"@type"			:"virtual-storage",
			"id"			: "$id",
			"name"			: "disk-$id",
			"size" 			: "${Sizes.toSize(sizeSpec)}",
			"expectations"	: ${expectationsToJson(expectations)}
		}
		""".stripMargin())

		TempDefs.instance.get().setData(tempName, id.toString())
		def response = client.execute(put)
		logResponse(response)
		assertResponseStatus(response, 200)

	}

	static String expectationsToJson(DataTable expectations) {
		StringBuilder builder = new StringBuilder('[')

		if(expectations.gherkinRows.size() > 1) {
			def rows = expectations.gherkinRows.subList(1, expectations.gherkinRows.size())
			def first = true
			for(DataTableRow row : rows) {
				def type = row.cells[0]
				def atts = row.cells[1]
				if(!first) {
					builder.append(',')
				}
				builder.append(""" { "@type":"$type", $atts } """)
			}
		}

		builder.append(']')
		return builder.toString()
	}

	@And("^session (\\S+): user can create a vm - generated id into into temp:(\\S+)")
	void createVm(String sessionId, String tempName, DataTable dataTable) {
		def client = Clients.instance.get().getClient(sessionId)
		def id = UUID.randomUUID()

		def props = toMap(dataTable)

		def storages = ""
		for(key in props.keySet()) {
			if(key.startsWith("storage-")) {
				def conn = props.get(key)
				def device = conn.substring(0, conn.indexOf(":"))
				def deviceTemp = conn.substring(conn.indexOf(":") + 1)
				if(!storages.isEmpty()) {
					storages += ","
				}
				storages += """
					{
						"virtualStorageId" : "${TempDefs.instance.get().getData(deviceTemp)}",
						"device" : "${device}",
						"bus" : "${ device == "cdrom" ? "sata" : "virtio" }"
					}
					"""
			}
		}

		def put = HttpDefs.instance.get().put("s/r/vm", """
		{
			"@type":"vm",
			"id" : "$id",
			"name" : "vm-$id",
			"memory" : {
				"min" : ${Sizes.toSize(props.get("memory-min"))},
				"max" : ${Sizes.toSize(props.get("memory-max"))}
			},
			"virtualStorageLinks" : [${storages}]
		}
		""".stripMargin())

		def response = client.execute(put)
		logResponse(response)
		assertResponseStatus(response, 200)
		TempDefs.instance.get().setData(tempName, id.toString())
	}

	static def assertResponseStatus(HttpResponse httpResponse, int code) {
		Assert.assertEquals("Expected response code $code", code, httpResponse.statusLine.statusCode)
	}

	private String logResponse(HttpResponse response) {
		scenario.write("status code is ${response.getStatusLine().statusCode}")
		if(response.entity != null) {
			def responseText = response.entity.content.newReader().text
			scenario.write("response body is ${responseText}")
			return responseText
		} else {
			scenario.write(" -- no response body")
			return ""
		}
	}

	@And("^session (\\S+): user can start the VM temp:(\\S+)")
	void startVm(String sessionId, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def vmId = UUID.fromString( TempDefs.instance.get().getData(tempName))

		def response = client.execute(HttpDefs.instance.get().post("s/r/vm/$vmId/start"))

		logResponse(response)
		assertResponseStatus(response, 204)
	}

	@And("^session (\\S+): the virtual machine temp:(\\S+) should start - tolerate (\\d+) second delay")
	void verifyVmStarts(String sessionId, String tempName, int toleranceSeconds) {
		def client = Clients.instance.get().getClient(sessionId)
		def vmId = UUID.fromString( TempDefs.instance.get().getData(tempName))

		def start = System.currentTimeMillis()
		def vmDyn = null
		while(vmDyn == null && start + (1000 * toleranceSeconds) > System.currentTimeMillis()) {
			Thread.sleep(1000)
			def response = client.execute(HttpDefs.instance.get().get("s/r/vm-dyn/$vmId"))
			def responseText = logResponse(response)
			if(response.statusLine.statusCode == 200) {
				vmDyn = responseText
			}
		}

		if(vmDyn == null) {
			Assert.fail("looks like the VM failed to start")
		}
	}

	@And("^session (\\S+): storage temp:(\\S+) should be allocated on host temp:(\\S+)")
	void verifyStorageAllocation(String sessionId, String storageTempName, String hostTempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def storageId = UUID.fromString( TempDefs.instance.get().getData(storageTempName))
		def hostId = UUID.fromString( TempDefs.instance.get().getData(hostTempName))

		def response = client.execute(HttpDefs.instance.get().get("s/r/virtual-storage-dyn/$storageId"))
		def responseText = logResponse(response)

		def responseJson = new ObjectMapper().readTree(responseText)

		def allocation = responseJson.get("allocation")
		//TODO

	}

	@And("^session (\\S+): virtual machine temp:(\\S+) should be started on host temp:(\\S+)")
	void verifyVmHost(String sessionId, String vmTempName, String hostTempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def vmId = UUID.fromString( TempDefs.instance.get().getData(vmTempName))
		def hostId = UUID.fromString( TempDefs.instance.get().getData(hostTempName))

		def response = client.execute(HttpDefs.instance.get().get("s/r/vm-dyn/$vmId"))
		def responseTxt = logResponse(response)

		def responseJson = new ObjectMapper().readTree(responseTxt)

		Assert.assertEquals((responseJson["hostId"] as ValueNode).textValue(), hostId.toString())
	}

	@And("^session (\\S+): lvm volume group name pattern:(.*)")
	void setLvmPattern(String sessionId, String pattern) {
		def client = Clients.instance.get().getClient(sessionId)

		def response = client.execute(HttpDefs.instance.get().get("s/r/config"))
		def jsonConfig = new ObjectMapper().readTree(logResponse(response))
		def storageTechnologies = jsonConfig["storageTechnologies"] as ObjectNode
		storageTechnologies
		//TODO
	}

	@And("^session (\\S+): all storage technologies disabled except (\\S+)")
	void disableStorageTechnologies(String sessionId, String enabledStorageTechnologies) throws Throwable {
		def client = Clients.instance.get().getClient(sessionId)
		def enabled = enabledStorageTechnologies.split(",")

		def response = client.execute(HttpDefs.instance.get().get("s/r/config"))
		def jsonConfig = new ObjectMapper().readTree(logResponse(response))
		def storageTechnologies = jsonConfig["storageTechnologies"] as ObjectNode

		for(prop in storageTechnologies.fields()) {
			if(prop.key.endsWith("Enabled") && storageTechnologies.get(prop.key).booleanValue()) {
				storageTechnologies.put(prop.key, false)
			}
		}

		for(prop in enabled) {
			storageTechnologies.put(prop + "Enabled",true)
		}

		def put = HttpDefs.instance.get()
				.put("s/r/config",
				new ObjectMapper()
						.configure(SerializationFeature.INDENT_OUTPUT, true)
						.writeValueAsString(jsonConfig))
		put.setHeader("Content-Type", "application/json")
		final def updateResponse = client.execute(put)
		logResponse(updateResponse)
	}

	@And("session (\\S+): recycle host by temp id (\\S+)")
	void setHostRecycle(String sessionId, String hostTempId) {
		def client = Clients.instance.get().getClient(sessionId)
		def hostId = TempDefs.instance.get().getData(hostTempId)
		def response = client.execute(HttpDefs.instance.get().get("s/r/host/$hostId"))


		def hostJsonTxt = logResponse(response)
		assertResponseStatus(response, 200)
		def mapper = new ObjectMapper()
		def jsonTree = mapper.readTree(hostJsonTxt) as ObjectNode
		jsonTree.put("recycling",true)
		def updateJson = mapper.writeValueAsString(jsonTree)

		def updateResponse = client.execute(HttpDefs.instance.get().post("s/r/host/$hostId", updateJson))
		logResponse(updateResponse)
	}

	@And("session (\\d+): virtual storage temp:(\\S+) should migrate to host temp:(\\S+) within (\\d+) seconds")
	def verifyStorageMigratedToHost(String sessionId, String storageTempId, String hostTempId, int seconds) {
		def client = Clients.instance.get().getClient(sessionId)
		def storageId = TempDefs.instance.get().getData(storageTempId)
		def hostId = TempDefs.instance.get().getData(hostTempId)
		def start = System.currentTimeMillis()

		while(System.currentTimeMillis() < start + (seconds * 1000)) {

			def response = client.execute(HttpDefs.instance.get().get("s/r/virtual-storage-dyn/$storageId"))
			def responseJson = logResponse(response)
			if(response.statusLine.statusCode == 200 && responseJson.contains(hostId)) {
				scenario.write("ok, that's what we were looking for")
				return
			}

			sleep(1000)
		}

		Assert.fail("migration did not happen in $seconds seconds")
	}

	@And("session (\\d+): and host temp:(\\S+) must be recycled within (\\d+) seconds")
	def verifyHostRecycled(String sessionId, String hostTempId, int seconds) {
		def client = Clients.instance.get().getClient(sessionId)
		def hostId = TempDefs.instance.get().getData(hostTempId)
		def start = System.currentTimeMillis()

		while (System.currentTimeMillis() < start + (seconds * 1000)) {
			def response = client.execute(HttpDefs.instance.get().get("s/r/host/$hostId"))
			logResponse(response)
			if(response.statusLine.statusCode == 404) {
				scenario.write("got 404 - meaning the host was recycled")
				return
			}
		}

		Assert.fail("recycling did not happen in $seconds seconds")
	}

}
