# External Integration Tests for Kerub

Some of the integration tests are in kerub main project.
These must be able to run anywhere (e.g. windows, no virtualization enabled, offline) and finish quick.
The tests grouped in external tests build do not have these requirements, they may build on OS-specific
packages and build on hardware capabilities.

Tests will start virtual machines and destroy them.

## VM image guidelines

### Image preparation checklist

 * There should be no public keys in ssh
 * __sshd__ must be enabled
 * there must be a user 'kerub-test' with password 'password' with __sudo__ permission
 * "Defaults requiretty" should be removed from /etc/sudoers (fedora and the like)
 * kerub-test user must be added to /etc/sudoers ```kerub-test (ALL:ALL) NOPASSWD:ALL```
 * all packages must be installed to perform it's function (see $use)
 * all other services should be disabled
 * image size should be as small as possible - use e.g. ```fstrim```
 * image should be compressed with ```xz -4```

### Creating the archive

 * tar is used because compression algorithms do not deal well with sparse files - tar does
 * in the tar file there must be a single image file with qcow2 format

### Naming convention

image file:
```v1/kerub-$operatingSystem-$osVersion-$use-$imageVersion.qcow2```
image archive:
```v1/kerub-$operatingSystem-$osVersion-$use-$imageVersion.tar.xz```
md5 checksum of the archive file
```v1/kerub-$operatingSystem-$osVersion-$use-$imageVersion.tar.xz.md5```

where
	* $operatingSystem: name of the OS / distribution - e.g. centos, ubuntu, freebsd
	* $osVersion: the release of the OS
	* $use: what the image can be used for - __node__ for the server running VM workloads, 
	__controller__ for the server running the kerub webappp and __any__ for both

#### Examples

```v1/kerub-centos-7-any-1.tar.xz```
```v1/kerub-freebsd-11-node-1.tar.xz```

