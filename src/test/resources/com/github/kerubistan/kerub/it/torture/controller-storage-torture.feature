Feature: Torturing the controller storage

  Scenario Outline: Just create a few million items without clustering
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB            |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <controller-image> / install-cmd
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	#not really a lot
	And session 1: user can create 500 virtual disks
	And session 1: user can read the virtual disks in random order 10 times
	And session 1: user can create 500 virtual networks
	And session 1: user can read the virtual networks in random order 10 times
	And session 1: user can create 500 virtual machines
	And session 1: user can read the virtual networks in random order 10 times

	Examples:
	  | controller-image |
	  | centos_7         |
	  | opensuse_42      |
