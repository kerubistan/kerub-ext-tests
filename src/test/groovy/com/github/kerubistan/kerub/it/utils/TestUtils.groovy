package com.github.kerubistan.kerub.it.utils

import cucumber.api.PendingException

class TestUtils {

	static def TODO(String reason) {
		throw new PendingException("not implemented: $reason")
	}
}