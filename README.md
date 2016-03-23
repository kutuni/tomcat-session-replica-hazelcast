# tomcat-session-replica-hazelcast
##session replication by hazelcast for tomcats

compiler depends:
tomcat-juli.jar
catalina.jar
servlet-api.jar
hazelcast-all-3.6.jar

###configure context.conf

##You can specify the following parameters in configuration file:

* `newInstance` false mean, jvm allready have a intialized hazelcast instance. Accessing instance from instaceName,
				        true mean, appender initialized new hazelcastclient instance. Using `multicast and interface values.
* `instanceName` if using with allready have instance options, need accessing hazelcast instance from `instanceName`.
* `grupName` Hazelcast Network Configuration. More info [Ref](http://http://docs.hazelcast.org/docs/3.6.1/manual/html-single/)
* `grupPassword` Hazelcast Network Configuration. [Ref](http://http://docs.hazelcast.org/docs/3.6.1/manual/html-single/)
* `multicast` Hazelcast Network Configuration. [Ref](http://http://docs.hazelcast.org/docs/3.6.1/manual/html-single/)
* `port` Hazelcast Network Configuration. [Ref](http://http://docs.hazelcast.org/docs/3.6.1/manual/html-single/)
* `log` more runtime logs.
  


##install:
copy context.xml to tomcat/conf directory.
copy generated jar to tomcat/lib

