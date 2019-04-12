package com.github.kerubistan.kerub.it.blocks.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kerubistan.kerub.it.blocks.scenario.ScenarioAccess
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.zip.GZIPInputStream

import static org.junit.Assert.*

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

Logger logger = LoggerFactory.getLogger(this.getClass().getName())

class HttpEnvironment {

	public String applicationRoot

	static InheritableThreadLocal<HttpEnvironment> instance = new InheritableThreadLocal<>()

	HttpUriRequest put(String url, String body) {
		ScenarioAccess.get().write("sending put to $applicationRoot/$url with body\n$body\n")
		return putSilent(url, body)
	}

	HttpPut putSilent(String url, String body) {
		def put = new HttpPut("$applicationRoot/$url")
		put.setEntity(new StringEntity(body))
		put.setHeader("Content-Type", "application/json")
		return put
	}

	HttpUriRequest post(String url, String body) {
		ScenarioAccess.get().write("sending post to $applicationRoot/$url with body\n$body\n")
		return postSilent(url, body)
	}

	HttpPost postSilent(String url, String body) {
		def post = new HttpPost("$applicationRoot/$url")
		post.setEntity(new StringEntity(body))
		post.setHeader("Content-Type", "application/json")
		return post
	}

	HttpUriRequest get(String url) {
		ScenarioAccess.get().write("sending get to $applicationRoot/$url ")
		return new HttpGet("$applicationRoot/$url")
	}

	HttpPost post(String url) {
		ScenarioAccess.get().write("sending post to $applicationRoot/$url ")
		return new HttpPost("$applicationRoot/$url")
	}

	HttpUriRequest getSilent(String url) {
		return new HttpGet("$applicationRoot/$url")
	}
}

World {
	def env = new HttpEnvironment()
	HttpEnvironment.instance.set(env)
	env
}

After {
	instance.remove()
}

When(~/^(\S+) is set as application root$/) { root ->
	def appUrlOverride = System.getProperty("kerub.ext.test.appurl")
	if (appUrlOverride != null) {
		ScenarioAccess.get().write("will use the override url: $appUrlOverride")
		applicationRoot = appUrlOverride
	} else {
		applicationRoot = root
	}
	ScenarioAccess.get().write("application url is now ${applicationRoot}")
}

Given(~/^if we wait for the url (\S+) to respond for max (\d+) seconds$/) { String url, int timeout ->
	def start = System.currentTimeMillis()
	def end = start + (timeout * 1000)
	scenario.write("attempted url $url - did not work")
	while (System.currentTimeMillis() < end) {
		try {
			new URL(url).openStream()
			ScenarioAccess.get().write("attempted url $url - worked! hmmm...")
			return
		} catch (IOException ioe) {
			Thread.sleep(1000)
			logger.info("still waiting for $url (${end - System.currentTimeMillis()} ms left)")
			ScenarioAccess.get().write("attempted url $url - did not work")
			//tolerated
		}
	}
	ScenarioAccess.get().write("Failed after $timeout secs")
	fail("$url did not respond within $timeout second")

}


Then(~/^http url (\S+) is served compressed$/) { String url ->
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

Then(~/^http url (\S+) is also cached$/) { String url ->
	def client = HttpClients.createDefault()
	def get = new HttpGet(url)
	get.addHeader("Accept-Encoding", "gzip")
	def response = client.execute(get)
	assertTrue(response.getHeaders("Cache-Control").length > 0)
}

Then(~/^session (\S+): user can login with (\S+) password (\S+)$/) {
	String sessionId, String username, String password ->
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
				response.allHeaders.find { it.name == "Set-Cookie" }
						.value.with { it.substring(it.indexOf("=") + 1, it.indexOf(";")) }
		)
		logger.info("logged in as $username")
}

Then(~/^session (\d+): user information is (\S+) with role (\S+)$/) {
	int sessionId, String expectedUserName, String expectedRolesCsv ->
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
