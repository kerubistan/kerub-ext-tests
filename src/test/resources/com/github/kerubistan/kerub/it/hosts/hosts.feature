Feature: Kerub host stories

  Scenario Outline: Kerub with single <controller-image> controller and <host-image> hosts - basic monitoring
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | lb-1             | 00:00:00:00:01:01 | 192.168.123.21 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	  | host-3           | 00:00:00:00:02:03 | 192.168.123.33 |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And virtual machine lb-1
	  | mac  | 00:00:00:00:01:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And virtual machine host-1
	  | mac  | 00:00:00:00:02:01 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 512 MiB           |
	And virtual machine host-2
	  | mac  | 00:00:00:00:02:02 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 512 MiB           |
	And virtual machine host-3
	  | mac  | 00:00:00:00:02:03 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 512 MiB           |
	And we wait until 192.168.123.11 comes online, timeout: 300 seconds
	And we wait until 192.168.123.21 comes online, timeout: 300 seconds
	And we wait until 192.168.123.31 comes online, timeout: 300 seconds
	And we wait until 192.168.123.32 comes online, timeout: 300 seconds
	And we wait until 192.168.123.33 comes online, timeout: 300 seconds
	And command executed on 192.168.123.11: <install-repo-cmd>
	And command executed on 192.168.123.11: <install-cmd>
	And command executed on 192.168.123.11: <start-cmd>
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.33
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pubkey
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pubkey
	And session 1: user can fetch public key for 192.168.123.33 into temp host-3-pubkey
	And session 1: user can connect to websocket
	And session 1: websocket subscribe to /host/
	And session 1: websocket subscribe to /host-dyn/
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pubkey and store ID in temp host-1-id
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pubkey and store ID in temp host-2-id
	And session 1: user can join host 192.168.123.33 using public key and fingerprint host-3-pubkey and store ID in temp host-3-id

	Examples:
	  | controller-image | host-image | install-repo-cmd                                                                                                              | install-cmd               | start-cmd                 |
	  | centos_7         | centos_7   | sudo sh -c "curl https://bintray.com/k0zka/kerub-centos/rpm > /etc/yum.repos.d/kerub.repo && cat /etc/yum.repos.d/kerub.repo" | sudo yum -y install kerub | sudo service tomcat start |
