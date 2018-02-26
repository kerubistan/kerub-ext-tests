package com.github.kerubistan.kerub.it.blocks.osimages

class OsImages {

	private static imageNames = [
	        "centos_7" : "com/github/kerubistan/kerub/it/centos_7/centos7.properties",
			"fedora_23" : "com/github/kerubistan/kerub/it/fedora_23/fedora_23.properties",
			"opensuse_42" : "com/github/kerubistan/kerub/it/opensuse_42/opensuse_42.properties"
	]

	static String getOsCommand(String imageName, String commandId)  {
		Properties properties = new Properties()
		def input = Thread.currentThread().getContextClassLoader().getResourceAsStream(imageNames.get(imageName))
		properties.load(input)
		input.close()
		return properties.get(commandId)
	}
}
