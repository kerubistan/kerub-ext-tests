Feature: Storage migration

  Scenario Outline: LVM to LVM migration (<host-image>)
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	And virtual disks
	  | name          | size |
	  | host-1-disk-1 | 1 TB |
	  | host-1-disk-2 | 1 TB |
	  | host-2-disk-1 | 1 TB |
	  | host-2-disk-2 | 1 TB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	  | extra-disk:vdc | host-1-disk-2     |
	And virtual machine host-2
	  | mac            | 00:00:00:00:02:02 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-2-disk-1     |
	  | extra-disk:vdc | host-2-disk-2     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log   |
	  | 192.168.123.32 | /root/.ssh/authorized_keys |
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we wait until 192.168.123.32 comes online with timeout: 300 seconds
	And we fetch basic linux host info from 192.168.123.31
	And we fetch basic linux host info from 192.168.123.32
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc
	And command executed on 192.168.123.32:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	# first add host-1
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	# create a tiny disk, because otherwise it will take ages
	# but with data, the allocation must exist
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	# now I surely know that the disk is allocated on host-1, as it is the only host, let's add host-2
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pk
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pk and store ID in temp host-2-id
	# now recycle host-1 to enforce storage migration to host-2
	And session 1: recycle host by temp id host-1-id
	# generous time-limit for the migration
	And session 1: virtual storage temp:iso-id should migrate to host temp:host-2-id within 180 seconds
	# that 60 seconds means after the storage was migrated
	And session 1: and host temp:host-1-id must be recycled within 60 seconds

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |


  Scenario Outline: LVM to LVM migration of read-only virtual storage (duplicate - deduplicate)
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	And virtual disks
	  | name          | size |
	  | host-1-disk-1 | 1 TB |
	  | host-1-disk-2 | 1 TB |
	  | host-2-disk-1 | 1 TB |
	  | host-2-disk-2 | 1 TB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	  | extra-disk:vdc | host-1-disk-2     |
	And virtual machine host-2
	  | mac            | 00:00:00:00:02:02 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 1024 MiB          |
	  | extra-disk:vdb | host-2-disk-1     |
	  | extra-disk:vdc | host-2-disk-2     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log   |
	  | 192.168.123.32 | /root/.ssh/authorized_keys |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we wait until 192.168.123.32 comes online with timeout: 300 seconds
	And we fetch basic linux host info from 192.168.123.31
	And we fetch basic linux host info from 192.168.123.32
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc
	And command executed on 192.168.123.32:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	# first add host-1
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	# create a tiny disk, because otherwise it will take ages
	# but with data, the allocation must exist
	And session 1: user can upload a ro raw file TinyCore-current.iso - generated id into into temp:iso-id
	# now I surely know that the disk is allocated on host-1, as it is the only host, let's add host-2
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pk
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pk and store ID in temp host-2-id
	# now recycle host-1 to enforce storage migration to host-2
	And session 1: recycle host by temp id host-1-id
	# generous time-limit for the migration
	And session 1: virtual storage temp:iso-id should migrate to host temp:host-2-id within 180 seconds
	# that 60 seconds means after the storage was migrated
	And session 1: and host temp:host-1-id must be recycled within 60 seconds

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |


  Scenario Outline: LVM (<source-host-image>) to Gvinum (<target-host-image>) migration of read-write virtual storage (dead migrate)
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	  | host-2           | 00:00:00:00:02:02 | 192.168.123.32 |
	And virtual disks
	  | name          | size |
	  | host-1-disk-1 | 1 TB |
	  | host-1-disk-2 | 1 TB |
	  | host-2-disk-1 | 1 TB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01   |
	  | net            | kerub-net-1         |
	  | disk           | <source-host-image> |
	  | ram            | 1024 MiB            |
	  | extra-disk:vdb | host-1-disk-1       |
	  | extra-disk:vdc | host-1-disk-2       |
	And virtual machine host-2
	  | mac            | 00:00:00:00:02:02   |
	  | net            | kerub-net-1         |
	  | disk           | <target-host-image> |
	  | ram            | 1024 MiB            |
	  | extra-disk:vdb | host-2-disk-1       |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log   |
	  | 192.168.123.32 | /root/.ssh/authorized_keys |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we wait until 192.168.123.32 comes online with timeout: 300 seconds
	And we fetch basic linux host info from 192.168.123.31
	And we fetch basic linux host info from 192.168.123.32
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc
	And command executed on 192.168.123.32: echo drive vtbd1 device /dev/vtbd1 >> /tmp/drives.txt
	And command executed on 192.168.123.32: sudo gvinum create -f /tmp/drives.txt
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	# first add host-1
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	# create a tiny disk, because otherwise it will take ages
	# but with data, the allocation must exist
	And session 1: user can upload a rw raw file TinyCore-current.iso - generated id into into temp:iso-id
	# now I surely know that the disk is allocated on host-1, as it is the only host, let's add host-2
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pk
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pk and store ID in temp host-2-id
	# now recycle host-1 to enforce storage migration to host-2
	And session 1: recycle host by temp id host-1-id
	# generous time-limit for the migration
	And session 1: virtual storage temp:iso-id should migrate to host temp:host-2-id within 180 seconds
	# that 60 seconds means after the storage was migrated
	And session 1: and host temp:host-1-id must be recycled within 60 seconds

	Examples:
	  | controller-image | source-host-image | target-host-image |
	  | centos_7         | centos_7          | freebsd_11        |
	  | centos_7         | centos_7          | freebsd_12        |
	  | centos_7         | ubuntu_18         | freebsd_12        |
	  | centos_7         | opensuse_42       | freebsd_12        |
