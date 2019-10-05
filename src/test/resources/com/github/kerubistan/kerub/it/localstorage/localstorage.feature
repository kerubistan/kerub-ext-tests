Feature: Local storage

  Scenario Outline: Local LVM images on <host-image>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
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
	  | ram            | 2048 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we fetch basic linux host info from 192.168.123.31
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: host identified by key host-1-id should have lvm storage capability registered with size around 1TB +-50GB
	  | property        | expected value |
	  | volumeGroupName | kerub-storage  |
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	And session 1: user can create a disk with size 10GB - generated id into into temp:disk-id
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
	And session 1: virtual machine temp:vm-id should be started on host temp:host-1-id

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |
	  | centos_7         | ubuntu_16   |

  Scenario Outline: Local thin LVM images on <host-image>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
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
	  | ram            | 2048 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
    And we will dump controller database on failure
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                                        | debug |
	  | org.apache.sshd.client.session.ClientSession                       | debug |
	  | com.github.kerubistan.kerub.host.distros.AbstractLinux             | debug |
	  | com.github.kerubistan.kerub.utils.junix.common.MonitorOutputStream | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb
	And we fetch basic linux host info from 192.168.123.31
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And session 1: controller config storageTechnologies.storageBenchmarkingEnabled set to false type boolean
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: host identified by key host-1-id should have lvm storage capability registered with size around 1TB +-50GB
	  | property        | expected value |
	  | volumeGroupName | kerub-storage  |
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	And session 1: user can create a disk with size 4TB - generated id into into temp:disk-id
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
	And session 1: virtual machine temp:vm-id should be started on host temp:host-1-id

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |
	  | centos_7         | ubuntu_16   |

  Scenario Outline: Local mirrored LVM images on <host-image>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	And virtual disks
	  | name          | size |
	  | host-1-disk-1 | 1 TB |
	  | host-1-disk-2 | 1 TB |
	  | host-1-disk-3 | 1 TB |
	  | host-1-disk-4 | 1 TB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 2048 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	  | extra-disk:vdd | host-1-disk-2     |
	  | extra-disk:vde | host-1-disk-3     |
	  | extra-disk:vdf | host-1-disk-4     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And we fetch basic linux host info from 192.168.123.31
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo lvm vgcreate kerub-storage /dev/vdb /dev/vdc /dev/vdd /dev/vde
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: host identified by key host-1-id should have lvm storage capability registered with size around 4TB +-50GB
	  | property        | expected value |
	  | volumeGroupName | kerub-storage  |
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	And session 1: user can create a disk with size 10GB - generated id into into temp disk-id with expectations
	  | type               | atts                                 |
	  | storage-redundancy | "outOfBox": "false", "nrOfCopies": 2 |
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
	And session 1: virtual machine temp:vm-id should be started on host temp:host-1-id

	Examples:
	  | controller-image | host-image  |
	  | centos_7         | centos_7    |
	  | centos_7         | opensuse_42 |
	  | centos_7         | ubuntu_18   |
	  | centos_7         | ubuntu_16   |


  Scenario Outline: Local filesystems on <host-image>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	  | host-1           | 00:00:00:00:02:01 | 192.168.123.31 |
	And virtual disks
	  | name          | size   |
	  | host-1-disk-1 | 200 GB |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 1024 MiB           |
	And virtual machine host-1
	  | mac            | 00:00:00:00:02:01 |
	  | net            | kerub-net-1       |
	  | disk           | <host-image>      |
	  | ram            | 2048 MiB          |
	  | extra-disk:vdb | host-1-disk-1     |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And we wait until 192.168.123.31 comes online with timeout: 300 seconds
	And <controller-image> package file uploaded to 192.168.123.11 directory /tmp
	And command template executed on 192.168.123.11: <controller-image> / install-pkg-cmd
	And kerub logger update on 192.168.123.11, root is info level
	  | com.github.kerubistan.kerub                  | debug |
	  | org.apache.sshd.client.session.ClientSession | debug |
	And command template executed on 192.168.123.11: <controller-image> / start-cmd
	And command executed on 192.168.123.31:sudo mkfs -t <filesystem> /dev/vdb <options>
	And command executed on 192.168.123.31:sudo mkdir /kerub
	And command executed on 192.168.123.31:sudo mount /dev/vdb /kerub
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then session 1: user can login with admin password password
	# this is actually to disable the logical volume created for the root filesystem
	And session 1: lvm volume group name pattern: kerub-.*
	# disable the fs benchmarking because it takes too long
	And session 1: controller config storageTechnologies.storageBenchmarkingEnabled set to false type boolean
	And session 1: user can download kerub controller public ssh key to temp controller-public-sshkey
	And Temporary controller-public-sshkey can be appended to /root/.ssh/authorized_keys on 192.168.123.31
	And session 1: user can fetch public key for 192.168.123.31 into temp host-1-pk
	And session 1: user can join host 192.168.123.31 using public key and fingerprint host-1-pk and store ID in temp host-1-id
	And session 1: host identified by key host-1-id should have fs storage capability registered with size around 200GB +-70GB
	  | property   | expected value |
	  | fsType     | <filesystem>   |
	  | mountPoint | /kerub         |
	And session 1: user can upload a raw file TinyCore-current.iso - generated id into into temp:iso-id
	And session 1: user can create a disk with size 10GB - generated id into into temp:disk-id
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
	And session 1: virtual machine temp:vm-id should be started on host temp:host-1-id

	Examples:
	  | controller-image | host-image  | filesystem | options    |
	  | centos_7         | centos_7    | ext3       | -Jsize=128 |
	  | centos_7         | opensuse_42 | ext4       | -Jsize=128 |
	  | centos_7         | ubuntu_18   | ext4       | -Jsize=128 |
	  | centos_7         | ubuntu_16   | ext4       | -Jsize=128 |
