package com.github.kerubistan.kerub.it.utils

class Environment {

	private final static String TEST_PROPS = "test.properties"

	private final static String HYPERVISOR_PROP = "hypervisor"
	private final static String HYPERVISOR_DEFAULT = "qemu:///system"

	private final static String STORAGE_HOST_DEFAULT = "localhost"
	private final static String STORAGE_HOST_PROP = "storage-host"

	private final static String STORAGE_HOST_USER_DEFAULT = "kerub-it-test"
	private final static String STORAGE_HOST_USER_PROP = "storage-host"

	private final static String STORAGE_HOST_PWD_DEFAULT = "password"
	private final static String STORAGE_HOST_PWD_PROP = "storage-password"

	private static Properties getTestProperties() {
		final Properties props = new Properties()
		final File inputFile= new File(TEST_PROPS)
		if(inputFile.exists()) {
			final InputStream input = new FileInputStream(inputFile)
			try {
				props.load(input)
			} finally {
				input.close()
			}
		}
		return props
	}

	static String getHypervisorUrl() {
		getTestProperties().getProperty(HYPERVISOR_PROP, HYPERVISOR_DEFAULT)
	}

	static String getStorageHost() {
		getTestProperties().getProperty(STORAGE_HOST_PROP, STORAGE_HOST_DEFAULT)
	}

	static String getStorageUser() {
		getTestProperties().getProperty(STORAGE_HOST_USER_PROP, STORAGE_HOST_USER_DEFAULT)
	}

	static String getStoragePassword() {
		getTestProperties().getProperty(STORAGE_HOST_PWD_PROP, STORAGE_HOST_PWD_DEFAULT)
	}

}