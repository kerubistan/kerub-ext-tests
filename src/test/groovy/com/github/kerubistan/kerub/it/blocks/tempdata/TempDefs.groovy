package com.github.kerubistan.kerub.it.blocks.tempdata

import cucumber.api.java.After
import cucumber.api.java.Before

class TempDefs {

	static instance = new InheritableThreadLocal<TempDefs>()

	private values = new HashMap<String, byte[]>()

	@Before
	void setup() {
		instance.set(this)
	}

	@After
	void cleanup() {
		instance.remove()
	}

	String getData(String key) {
		return values.get(key)
	}

	void setData(String key, String data) {
		values.put(key, data)
	}
}
