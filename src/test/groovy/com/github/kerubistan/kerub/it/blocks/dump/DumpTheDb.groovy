package com.github.kerubistan.kerub.it.blocks.dump

import com.github.kerubistan.kerub.it.blocks.http.Clients
import com.github.kerubistan.kerub.it.blocks.http.HttpEnvironment
import cucumber.api.Scenario
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import org.apache.commons.io.IOUtils
import org.apache.tools.tar.TarInputStream

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

enum DumpOn {
	Success,
	Failure,
	Always
}

class DumpEnvironment {
	DumpOn dumpOn = DumpOn.Failure
}

World {
	new DumpEnvironment()
}

Given(~/^we will dump controller database (on success|on failure|always)$/) {
	String dumpWhen ->
		if (dumpWhen == "on success") {
			dumpOn = DumpOn.Success
		} else if (dumpWhen == "on failure") {
			dumpOn = DumpOn.Failure
		} else if (dumpWhen == "always") {
			dumpOn = DumpOn.Always
		} else {
			throw new IllegalArgumentException("not accepted: $dumpWhen")
		}
}

After(Integer.MAX_VALUE) {
	Scenario scenario ->
		if (dumpOn == DumpOn.Always
				|| (dumpOn == DumpOn.Failure && scenario.isFailed())
				|| (dumpOn == DumpOn.Success && !scenario.isFailed())) {
			scenario.write("Scenario failed, attaching database dump")
			def httpEnv = HttpEnvironment.instance.get()
			def client = Clients.instance.get().getClient("1")
			def response = client.execute(httpEnv.get("s/r/admin/support/db-dump"))
			scenario.println("db dump response: ${response.statusLine.statusCode}")

			def tar = new TarInputStream(response.entity.content)
			def entry = tar.nextEntry

			while (entry != null) {
				scenario.println()
				def temp = new ByteArrayOutputStream()
				temp.write("dump file ${entry.name} - ${entry.size}\n-----------------\n".bytes)
				IOUtils.copy(tar, temp)
				scenario.embed(temp.toByteArray(), "text/plain")

				entry = tar.nextEntry
			}

		}
}