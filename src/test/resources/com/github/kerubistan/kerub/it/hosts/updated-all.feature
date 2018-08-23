Feature: Kerub quick test after controller operating system update

  Scenario Outline: Kerub with single <controller-image> controller and single <host-image> host, both with latest version of all and everything
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And virtual machine host-1
	  | mac  | 00:00:00:00:02:01 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 512 MiB           |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online, timeout: 300 seconds
	And command template executed on 192.168.123.11: <controller-image> / full-update-cmd
	And we wait 30 seconds
	And we wait until 192.168.123.11 comes online, timeout: 300 seconds
	#TODO something wrong with the ssh connection  if we do not wait a while after reboot
	And we wait 15 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And we wait until 192.168.123.31 comes online, timeout: 300 seconds
	And we wait until 192.168.123.31 comes online, timeout: 300 seconds
	And command template executed on 192.168.123.11: <controller-image> / install-repo-cmd
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pubkey
	And session 1: user can connect to websocket
	And session 1: websocket subscribe to /host/
	And session 1: websocket subscribe to /host-dyn/
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pubkey and store ID in temp host-1-id

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | opensuse_42      | opensuse_42 |
