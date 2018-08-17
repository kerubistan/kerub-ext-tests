Feature: Kerub clustering

  Scenario Outline: Kerub running on two <OS-image> servers with a <load-balancer> load-balancer and <servlet-container>
	Given virtual network kerub-net-1 domain name kerub.it
	  | host    | mac               | ip             |
	  | kerub-1 | 00:00:00:00:00:01 | 192.168.123.11 |
	  | kerub-2 | 00:00:00:00:00:02 | 192.168.123.12 |
	  | lb-1    | 00:00:00:00:01:01 | 192.168.123.21 |
	  | ldap-1  | 00:00:00:00:02:01 | 192.168.123.31 |
	And virtual machine kerub-1
	  | mac  | 00:00:00:00:00:01 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 512 MiB           |
	And virtual machine kerub-2
	  | mac  | 00:00:00:00:00:02 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 512 MiB           |
	And virtual machine lb-1
	  | mac  | 00:00:00:00:01:01 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 512 MiB           |
	And virtual machine ldap-1
	  | mac  | 00:00:00:00:02:01 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 512 MiB           |
	And we wait until 192.168.123.11 comes online, timeout: 240 seconds
	And we wait until 192.168.123.12 comes online, timeout: 240 seconds
	And we wait until 192.168.123.21 comes online, timeout: 240 seconds
	And we wait until 192.168.123.31 comes online, timeout: 240 seconds
	And command template executed on 192.168.123.11: <OS-image> / install-pkg-cmd
	And command template executed on 192.168.123.12: <OS-image> / install-pkg-cmd
	And command template executed on 192.168.123.11: <OS-image> / start-cmd
	And command template executed on 192.168.123.12: <OS-image> / start-cmd
	And command template executed on 192.168.123.11: <OS-image> / configure-session-replication
	And command template executed on 192.168.123.12: <OS-image> / configure-session-replication
	And command executed on 192.168.123.11: echo TODO: setup kerub infinispan cluster
	And command executed on 192.168.123.12: echo TODO: setup kerub infinispan cluster
	And command template executed on 192.168.123.31: <OS-image> / <install-ldap-cmd>
	And file on 192.168.123.21: /tmp/lbconfig.conf generated from <lb-cfg-template> using parameters
	  | backend_ips | csv | 192.168.123.11,192.168.123.12 |
	And command executed on 192.168.123.21: sudo sh -c "cat /tmp/lbconfig.conf >> <lb-cfg-path> && cat <lb-cfg-path>"
	And command executed on 192.168.123.21: <start-load-balancer>
 #this 360 sec timeout  is silly but with that fucking archaic tomcat it takes ages
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	And if we wait for the url http://192.168.123.12:8080/ to respond for max 360 seconds
	When http://192.168.123.21/ is set as application root
	Then session 1: user can login with testadmin-1 password password
	And session 1: user information is testadmin-1 with role Admin
	And session 1: user can connect to websocket
	And session 2: user can login with testadmin-2 password password
	And session 2: user can connect to websocket
	And session 2: user information is testadmin-2 with role Admin
	And session 1: websocket subscribe to /vnet/
	And session 2: create test vnet
	And session 1: gets a create message with vnet
	And session 2: update test vnet
	And session 1: gets an update message with vnet

	Examples:
	  | OS-image |
	  | centos_7 |
#	  | fedora_23   | jetty             | haproxy       | sudo dnf -y install kerub | sudo sh -c "curl https://bintray.com/k0zka/kerub-fedora/rpm > /etc/yum.repos.d/kerub.repo" | echo TODO: session replication | sudo service jetty9 start |
#	  | ubuntu_16   | jetty             | haproxy       | sudo apt-get -y install   |                                                                                            | echo TODO: session replication |                           |
#	  | opensuse_42 | tomcat            | haproxy       | sudo zypper -y install    | echo TODO > /etc/zypp/repos.d/kerub.repo                                                   | echo TODO: session replication |                           |
#	  | freebsd_11  | jetty             | haproxy       | sudo pkg install          | TODO                                                                                       | echo TODO                      |                           |
