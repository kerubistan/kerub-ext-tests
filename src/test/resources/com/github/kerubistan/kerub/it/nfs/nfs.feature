Feature: Nightmare Filesystem

  Scenario Outline: NFS sharing images
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
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 512 MiB           |
	  | extra-disk:vdb | host-1-disk-1     |
	And virtual machine host-2
	  | mac  | 00:00:00:00:02:02 |
	  | net  | kerub-net-1       |
	  | disk | <host-image>      |
	  | ram  | 2048 MiB          |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	  | 192.168.123.31 | /etc/exports             |
	  | 192.168.123.32 | /etc/exports             |
    And we will dump controller database on failure
	And we wait until 192.168.123.11 comes online with timeout: 180 seconds
	And we wait until 192.168.123.31 comes online with timeout: 180 seconds
	And we wait until 192.168.123.32 comes online with timeout: 180 seconds
	And we fetch basic linux host info from 192.168.123.31
	And we fetch basic linux host info from 192.168.123.32
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo mkfs.ext4 -F /dev/vdb
	And command executed on 192.168.123.31:sudo mkdir /kerub
	And command executed on 192.168.123.31:sudo bash -c "echo /dev/vdb /kerub	ext4	defaults	0	2 >> /etc/fstab"
	And command executed on 192.168.123.31:sudo mount /kerub
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: all storage technologies disabled except nfs
	And session 1: lvm volume group name pattern: kerub-.*
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.32
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: user can fetch public key for 192.168.123.32 into temp host-2-pk
	And session 1: user can join host 192.168.123.32 using public key and fingerprint host-2-pk and store ID in temp host-2-id
	And session 1: host identified by key host-1-id should have fs storage capability registered with size around 1TB +-75GB
	  | property   | expected value |
	  | mountPoint | /kerub         |
	  | fsType     | ext4           |
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	And session 1: user can create a disk with size 20GB - generated id into into temp:disk-id
	And session 1: user can create a vm - generated id into into temp:vm-id
	  | param      | value        |
	  | storage-1  | cdrom:iso-id |
	  | storage-2  | disk:disk-id |
	  | memory-min | 1 GB         |
	  | memory-max | 1 GB         |
	And session 1: user can start the VM temp:vm-id
	And session 1: the virtual machine temp:vm-id should start - tolerate 60 second delay
	# since there is no physical storage elsewhere
	And session 1: storage temp:disk-id should be allocated on host temp:host-1-id
	And session 1: storage temp:iso-id should be allocated on host temp:host-1-id
	# since there is no sufficient memory elsewhere
	And session 1: virtual machine temp:vm-id should be started on host temp:host-2-id

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | centos_6    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_16   |
	  | centos_7         | ubuntu_18   |
