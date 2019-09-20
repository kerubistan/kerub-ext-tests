Feature: tons of file uploads

  Scenario Outline: some small file uploads on <host-image>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	And virtual disks
	  | name          | size   |
	  | host-1-disk-1 | 128 MB |
	  | host-2-disk-1 | 128 MB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01 |
	  | net  | kerub-net-1       |
	  | disk | centos_7          |
	  | ram  | 1024 MiB          |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	And virtual machine host-2
	  | mac            | 00:00:00:00:02:02 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-2-disk-1     |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <controller-image> / install-cmd
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we wait until 192.168.123.32 comes online with timeout: 300 seconds
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb
	And command executed on 192.168.123.32:sudo lvm vgcreate kerub-storage /dev/vdb
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pubkey
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pubkey and store ID in temp host-1-id
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pubkey
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pubkey and store ID in temp host-2-id
	And session 1: user can upload a ro raw file TinyCore-current.iso 10 times

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
      | centos_7         | ubuntu_18   |

  Scenario Outline: some small file uploads and delete on <host-image> and FS
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	And virtual disks
	  | name          | size   |
	  | host-1-disk-1 | 128 MB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01 |
	  | net  | kerub-net-1       |
	  | disk | centos_7          |
	  | ram  | 1024 MiB          |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <controller-image> / install-cmd
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And command executed on 192.168.123.31:sudo mkfs.ext4 /dev/vdb
	And command executed on 192.168.123.31:sudo mkdir /kerub
	And command executed on 192.168.123.31:sudo mount /dev/vdb /kerub
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pubkey
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pubkey and store ID in temp host-1-id
	And session 1: user can upload and delete a ro raw file TinyCore-current.iso 100 times

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |

#  Scenario Outline: tons of file uploads on <host-image>
#	Given virtual network kerub-net-1 domain name kerub.it
#	  | host             | mac               | ip             |
#	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
#	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
#	  | host-2           | 00:00:00:00:02:02 | 192.168.123.31 |
#	And virtual disks
#	  | name          | size |
#	  | host-1-disk-1 | 1 TB |
#	  | host-2-disk-1 | 1 TB |
#	And virtual machine kerub-controller
#	  | mac  | 00:00:00:00:00:01 |
#	  | net  | kerub-net-1       |
#	  | disk | centos_7          |
#	  | ram  | 1024 MiB          |
#	And virtual machine host-1
#	  | mac            | 00:00:00:00:02:01 |
#	  | net            | kerub-net-1       |
#	  | disk           | <host-image>      |
#	  | ram            | 2048 MiB          |
#	  | extra-disk:vdb | host-1-disk-1     |
#	And virtual machine host-2
#	  | mac            | 00:00:00:00:02:01 |
#	  | net            | kerub-net-1       |
#	  | disk           | <host-image>      |
#	  | ram            | 2048 MiB          |
#	  | extra-disk:vdb | host-2-disk-1     |
#
#
#	Examples:
#	  | host-image |