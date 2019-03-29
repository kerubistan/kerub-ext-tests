Feature: The controller should be ok with restarts

  Scenario Outline: unclustered controller should be ok after any number of restarts
	Given virtual network kerub-net-1 domain name kerub.it
	  | host             | mac               | ip             |
	  | kerub-controller | 00:00:00:00:00:01 | 192.168.123.11 |
	And virtual machine kerub-controller
	  | mac  | 00:00:00:00:00:01  |
	  | net  | kerub-net-1        |
	  | disk | <controller-image> |
	  | ram  | 512 MiB            |
	And we will attach the following log files at the end of the scenario
	  | 192.168.123.11 | /var/log/kerub/kerub.log |
	And we wait until 192.168.123.11 comes online with timeout: 300 seconds
	And command executed on 192.168.123.11: <install-repo-cmd>
	And command executed on 192.168.123.11: <install-cmd>
	And command executed on 192.168.123.11: <start-cmd>
	And command executed on 192.168.123.11: <enable-cmd>
	And if we wait for the url http://192.168.123.11:8080/ to respond for max 360 seconds
	When http://192.168.123.11:8080/ is set as application root
	Then the controller can be restarted 20 times with timeout 600 seconds, it will still work fine

