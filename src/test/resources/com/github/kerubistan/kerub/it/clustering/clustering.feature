Feature: Kerub clustering

  Scenario Outline: Kerub running on two <OS-image> servers with a <load-balancer> load-balancer
	Given virtual network kerub-net-1 domain name kerub.it
 	  | host    | mac               | ip             |
	  | kerub-1 | 00:00:00:00:00:01 | 192.168.123.11 |
	  | kerub-2 | 00:00:00:00:00:02 | 192.168.123.12 |
	  | lb-1    | 00:00:00:00:01:01 | 192.168.123.21 |
	And virtual machine kerub-1
	  | mac  | 00:00:00:00:00:01 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 256 MiB           |
	And virtual machine kerub-2
	  | mac  | 00:00:00:00:00:02 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 256 MiB           |
	And virtual machine lb-1
	  | mac  | 00:00:00:00:01:01 |
	  | net  | kerub-net-1       |
	  | disk | <OS-image>        |
	  | ram  | 256 MiB           |
	And we wait until 192.168.123.11 comes online, timeout: 240 seconds
	And we wait until 192.168.123.12 comes online, timeout: 240 seconds
	And we wait until 192.168.123.21 comes online, timeout: 240 seconds
	And command executed on 192.168.123.11: <install-repo-cmd>
	And command executed on 192.168.123.12: <install-repo-cmd>
	And command executed on 192.168.123.11: <install-cmd> kerub
	And command executed on 192.168.123.12: <install-cmd> kerub
	And command executed on 192.168.123.11: <configure-session-replication>
	And command executed on 192.168.123.12: <configure-session-replication>
	And command executed on 192.168.123.11: echo TODO: setup kerub infinispan cluster
	And command executed on 192.168.123.12: echo TODO: setup kerub infinispan cluster
	And command executed on 192.168.123.11: systemctl start <servlet-container>
	And command executed on 192.168.123.12: systemctl start <servlet-container>
	And command executed on 192.168.123.21: systemctl start <load-balancer>
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 240 seconds
	And if we wait for the url http://192.168.123.12:8080/ to respond for max 240 seconds
	When http://192.168.123.21/ is set as application root
	Then user can login with

	Examples:
	  | OS-image  | servlet-container | load-balancer | install-cmd    | install-repo-cmd                        | configure-session-replication  |
	  | centos_7  | tomcat            | haproxy       | yum -y install | echo TODO: setup yum repo               | echo TODO: session replication |
	  | fedora_23 | jetty             | haproxy       | dnf -y install | echo TODO > /etc/yum.repos.d/kerub.repo | echo TODO: session replication |
