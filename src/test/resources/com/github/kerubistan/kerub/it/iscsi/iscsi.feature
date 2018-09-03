Feature: The worst palindrome

  Scenario Outline: ISCSI sharing LVM images
	# obviously this story os only for linux hosts and BSD / other hosts need another story for
	# their volume manager software
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	And virtual disks
	  | name          | size |
	  | host-1-disk-1 | 1 TB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And virtual machine host-1
	  | mac        | 00:00:00:00:02:01 |
	  | net        | kerub-net-1       |
	  | disk       | <host-image>      |
	  | ram        | 512 MiB           |
	  | extra-disk | host-1-disk-1     |
	And virtual machine host-2
	  | mac  | 00:00:00:00:02:02 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 2048 MiB          |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online, timeout: 300 seconds
	And we wait until 192.168.123.31 comes online, timeout: 300 seconds
	And we wait until 192.168.123.32 comes online, timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pk
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pk and store ID in temp host-2-id
	And session 1: host identified by key host-1-id should have lvm storage capability registered with size around 1TB +-50GB
	  | property        | expected value |
	  | volumeGroupName | kerub-storage  |
	And I have to finish this story

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
