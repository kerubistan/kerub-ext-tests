Feature: Nightmare Filesystem

  Scenario Outline: NFS sharing images
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	  | host-3           | 00:00:00:00:02:03 | 192.168.123.33 |
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
	  | extra-disk | host-1-disk1      |
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
	And command executed on 192.168.123.31:mkfs.ext4 -F /dev/vdb
	And command executed on 192.168.123.31:mkdir /kerub
	And command executed on 192.168.123.31:echo  >> /etc/fstab
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then I have to finish this story


	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
