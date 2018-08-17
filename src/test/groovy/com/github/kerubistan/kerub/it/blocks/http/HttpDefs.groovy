package com.github.kerubistan.kerub.it.blocks.http

import com.fasterxml.jackson.databind.ObjectMapper
import cucumber.api.Scenario
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory

import java.util.zip.GZIPInputStream

import static org.junit.Assert.*

class HttpDefs {

	Scenario scenario = null

	static InheritableThreadLocal<HttpDefs> instance = new InheritableThreadLocal<>()

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
		instance.set(this)
	}

	@After
	void cleanup() {
		instance.remove()
	}

	String applicationRoot

	private final static logger = LoggerFactory.getLogger(HttpDefs)

	@When("(\\S+) is set as application root")
	void setApplicationRoot(String applicationRoot) {
		this.applicationRoot = applicationRoot
	}

	@Given("if we wait for the url (\\S+) to respond for max (\\d+) seconds")
	void waitForUrl(String url, int timeout) {
		def start = System.currentTimeMillis()
		def end = start + (timeout * 1000)
		while (System.currentTimeMillis() < end) {
			try {
				new URL(url).openStream()
				return
			} catch (IOException ioe) {
				Thread.sleep(1000)
				logger.info("still waiting for $url (${end - System.currentTimeMillis()} ms left)")
				scenario.write("attempted url $url - did not work")
				//tolerated
			}
		}
		scenario.write("Failed after $timeout secs")
		fail("$url did not respond within $timeout second")
	}

	HttpUriRequest put(String url, String body) {
		scenario.write("sending put to $applicationRoot/$url with body\n$body\n")
		return putSilent(url, body)
	}

	HttpPut putSilent(String url, String body) {
		def put = new HttpPut("$applicationRoot/$url")
		put.setEntity(new StringEntity(body))
		put.setHeader("Content-Type", "application/json")
		return put
	}

	HttpUriRequest post(String url, String body) {
		scenario.write("sending post to $applicationRoot/$url with body\n$body\n")
		return postSilent(url, body)
	}

	HttpPost postSilent(String url, String body) {
		def post = new HttpPost("$applicationRoot/$url")
		post.setEntity(new StringEntity(body))
		post.setHeader("Content-Type", "application/json")
		return post
	}

	HttpUriRequest get(String url) {
		scenario.write("sending get to $applicationRoot/$url ")
		return new HttpGet("$applicationRoot/$url")
	}

	HttpUriRequest getSilent(String url) {
		return new HttpGet("$applicationRoot/$url")
	}

	@Then("http url (\\S+) is served compressed")
	void verifyHttpCompression(String url) {
		def client = HttpClients.createDefault()
		def get = new HttpGet(url)
		get.addHeader("Accept-Encoding", "gzip")
		def response = client.execute(get)
		assertEquals("gzip", response.getFirstHeader(
				"Content-Encoding"
		).value)
		new GZIPInputStream(response.entity.content).readLines()

		response.close()
	}

	@Then("http url (\\S+) is also cached")
	void verifyHttpCaching(String url) {
		def client = HttpClients.createDefault()
		def get = new HttpGet(url)
		get.addHeader("Accept-Encoding", "gzip")
		def response = client.execute(get)
		assertTrue(response.getHeaders("Cache-Control").length > 0)
	}

	@Then("session (\\S+): user can login with (\\S+) password (\\S+)")
	void verifyUserLogin(String sessionId, String username, String password) {
		def client = Clients.instance.get().getClient(sessionId)
		def response = client.execute(
				post(
						"/s/r/auth/login",
						"""{"username":"$username","password":"$password"}"""
				)
		)

		assertEquals(204, response.getStatusLine().statusCode)
		Clients.instance.get().setHttpSessionId(
				sessionId,
				response.allHeaders.find {it.name == "Set-Cookie"}
						.value.with { it.substring(it.indexOf("=") + 1, it.indexOf(";")) }
		)
	}

	@Then("session (\\d+): user information is (\\S+) with role (\\S+)")
	void verifyUserInformation(int sessionId, String expectedUserName, String expectedRolesCsv) {
		def client = Clients.instance.get().getClient(sessionId.toString())
		def response = client.execute(
				get(
						"/s/r/auth/user"
				)
		)

		assertEquals(200, response.getStatusLine())

		def tree = new ObjectMapper().readTree(response.getEntity().content)
		assertEquals(expectedUserName, tree["name"])
		assertEquals(expectedUserName, tree["Roles"])

	}
}
