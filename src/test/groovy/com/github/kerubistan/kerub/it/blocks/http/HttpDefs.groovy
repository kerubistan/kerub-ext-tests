package com.github.kerubistan.kerub.it.blocks.http

import cucumber.api.java.en.Given
import org.junit.Assert
import org.slf4j.LoggerFactory

class HttpDefs {

	private final static logger = LoggerFactory.getLogger(HttpDefs)

	@Given("if we wait for the url (\\S+) to respond for max (\\d+) seconds")
	void waitForUrl(String url, int timeout) {
		def start = System.currentTimeMillis()
		def end = start + (timeout * 1000)
		while(System.currentTimeMillis() < end) {
			try {
				new URL(url).openStream()
				return
			} catch (IOException ioe) {
				Thread.sleep(1000)
				logger.info("still waiting for $url (${end - System.currentTimeMillis()} ms left)")
				//tolerated
			}
		}
		Assert.fail("$url did not respond within $timeout second")
	}
}
