Feature: Torturing the controller storage

  Scenario Outline: Just create a few million items without clustering
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online, timeout: 300 seconds
	And command executed on 192.168.123.11: <install-repo-cmd>
	And command executed on 192.168.123.11: <install-cmd>
	And command executed on 192.168.123.11: <start-cmd>
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	#not really a lot
	And session 1: user can create 1000 virtual networks
	And session 1: user can create 1000 virtual disks
	And session 1: user can create 1000 virtual machines
	And session 1: user can read the 1000 virtual machines in random order
	And session 1: user can read the 1000 virtual disks in random order
	And session 1: user can read the 1000 virtual networks in random order

	Examples:
	  | controller-image | install-repo-cmd                                                                                                              | install-cmd               | start-cmd                 |
	  | centos_7         | sudo sh -c "curl https://bintray.com/k0zka/kerub-centos/rpm > /etc/yum.repos.d/kerub.repo && cat /etc/yum.repos.d/kerub.repo" | sudo yum -y install kerub | sudo service tomcat start |
