package com.github.kerubistan.kerub.it.sizes

class Sizes {
	public static final def KB = 1024
	public static final def MB = 1024 * KB
	public static final def GB = 1024 * MB
	public static final def TB = 1024 * GB
	public static final def PB = 1024 * TB
	static BigInteger toSize(String spec) {
		if(spec.endsWith("KB")) {
			return spec.replaceAll("KB","").trim().toBigInteger() * KB
		} else if (spec.endsWith( "MB")) {
			return spec.replaceAll("MB","").trim().toBigInteger() * MB
		} else if (spec.endsWith( "GB")) {
			return spec.replaceAll("GB","").trim().toBigInteger() * GB
		} else if (spec.endsWith( "TB")) {
			return spec.replaceAll("TB","").trim().toBigInteger() * TB
		} else if (spec.endsWith( "PB")) {
			return spec.replaceAll("PB","").trim().toBigInteger() * PB
		}
	}
}
