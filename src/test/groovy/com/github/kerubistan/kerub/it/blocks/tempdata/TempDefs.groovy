package com.github.kerubistan.kerub.it.blocks.tempdata

import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

def instance = new InheritableThreadLocal<TempEnvironment>()

class TempEnvironment {

	private values = new HashMap<String, String[]>()

	String getData(String key) {
		return values.get(key)
	}

	void setData(String key, String data) {
		values.put(key, data)
	}

}

World {
	instance.set(new TempEnvironment())
	instance.get()
}

