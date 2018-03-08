Feature: Spark Batch Remote Debug Job Testing

  Scenario: factory helper unit test
    Given create batch Spark Job with driver debugging for 'http://localhost:8998/batches' with following parameters
      | spark.driver.extraJavaOptions | -head -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1234 -tail |
    Then throw exception 'com.microsoft.azure.hdinsight.spark.common.DebugParameterDefinedException' with message 'The driver Debug parameter is defined in Spark job configuration: -head -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1234 -tail'

    And create batch Spark Job with driver debugging for 'http://localhost:8998/batches' with following parameters
      | spark.yarn.maxAppAttempts | 3 |
    Then throw exception 'com.microsoft.azure.hdinsight.spark.common.DebugParameterDefinedException' with message 'The spark.yarn.maxAppAttempts is defined in Spark job configuration: 3'

    Given setup a mock livy service for POST request '/batches' to return '{"id":9, "state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"https://spkdbg.azurehdinsight.net/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And create batch Spark Job with driver debugging for '/batches' with following parameters
      | spark.driver.extraJavaOptions | -head |
    Then the Spark driver JVM option should be '-head -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0'
    Then the Spark driver max retries should be '1'

    Given setup a mock livy service for POST request '/batches' to return '{"id":9, "state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"https://spkdbg.azurehdinsight.net/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And create batch Spark Job with driver debugging for '/batches' with following parameters
      | | |
    Then the Spark driver JVM option should be '-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0'

  Scenario: getSparkJobApplicationId integration test with mocked Livy server
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,"state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"https://spkdbg.azurehdinsight.net/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    Then getting spark job url '/batch', batch ID 9's application id should be 'application_1492415936046_0015'
    Then getting spark job url '/batch', batch ID 9's driver log URL should be 'https://spkdbg.azurehdinsight.net/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy'

  Scenario: getSparkJobApplicationId negative integration test with broken Livy response
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,' with status code 200
    Then getting spark job url '/batch', batch ID 9's application id, '/batch/9' should be got with 3 times retried

  Scenario: getSparkJobApplicationId retries test with mocked Livy server
    Given setup a mock livy service for GET request '/batch/9' to return '{}' with status code 404
    Then getting spark job url '/batch', batch ID 9's application id, '/batch/9' should be got with 3 times retried

  Scenario: parsingAmHostHttpAddressHost unit tests
    Then Parsing driver HTTP address 'host.domain.com:8042' should get host 'host.domain.com'
    Then Parsing driver HTTP address '10.0.0.15:30060' should get host '10.0.0.15'
    Then Parsing driver HTTP address '10.0.0.15:' should be null
    Then Parsing driver HTTP address ':1234' should be null

  Scenario: parsingJvmDebuggingPort unit test
    Given parsing JVM debugging port should be 6006 for the following listens:
      | Listening for transport dt_socket at address: 6006 |
    Then parsing JVM debugging port should be -1 for the following listens:
      | <empty> |

  Scenario: getSparkDriverDebuggingPort integration test with responses
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,"state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"http://127.0.0.1:$port/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And setup a mock livy service for GET request '/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy/stdout?start=-4096' to return '<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd"> <html> <meta http-equiv="X-UA-Compatible" content="IE=8"> <meta http-equiv="Content-type" content="text/html; charset=UTF-8"> <title> Logs for container_e03_1492780173422_0013_02_000001 </title>   <table id="layout" class="ui-widget-content"> <thead> <tr> <td colspan="2"> <div id="header" class="ui-widget"> <div id="user"> Logged in as: dr.who </div> <div id="logo"> <img src="/yarnui/static/hadoop-st.png"> </div> <h1> Logs for container_e03_1492780173422_0013_02_000001 </h1> </div> </td> </tr> </thead> <tfoot> <tr> <td colspan="2"> <div id="footer" class="ui-widget"> </div> </td> </tr> </tfoot> <tbody> <tr> <td id="navcell"> <div id="nav"> <h3> ResourceManager </h3> <ul> <li> <a href="/yarnui/hn/">RM Home</a> </ul> <h3> NodeManager </h3> <ul> <li> <a href="/yarnui/10.0.0.15/node/node">Node Information</a> <li> <a href="/yarnui/10.0.0.15/node/allApplications">List of Applications</a> <li> <a href="/yarnui/10.0.0.15/node/allContainers">List of Containers</a> </ul> <h3> Tools </h3> <ul> <li> <a href="/yarnui/10.0.0.15/conf">Configuration</a> <li> <a href="/yarnui/10.0.0.15/logs">Local logs</a> <li> <a href="/yarnui/10.0.0.15/stacks">Server stacks</a> <li> <a href="/yarnui/10.0.0.15/jmx?qry=Hadoop:*">Server metrics</a> </ul> </div> </td> <td class="content"> <p> Log Type: stdout <pre>Listening for transport dt_socket at address: 6006 </pre> </td> </tr> </tbody> </table> </html>' with status code 200
    Then getting Spark driver debugging port from URL '/batch', batch ID 9 should be 6006

  Scenario: getSparkDriverDebuggingPort negative integration test with responses for bad yarnui response
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,"state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"http://127.0.0.1:$port/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And setup a mock livy service for GET request '/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy/stdout?start=-4096' to return '<html> </html>' with status code 307
    Then getting Spark driver debugging port from URL '/batch', batch ID 9 should be 0
    Then throw exception 'java.net.UnknownServiceException' with checking type only

  Scenario: getSparkDriverHost integration test with responses
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,"state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"http://127.0.0.1:$port/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And setup a mock livy service for GET request '/yarnui/ws/v1/cluster/apps/application_1492415936046_0015' to return '{ "app": { "amNodeLabelExpression": "", "finishedTime": 1493100184345, "startedTime": 1493097873053, "priority": 0, "applicationTags": "livy-batch-15-bcixsxv0", "applicationType": "SPARK", "clusterId": 1492780173422, "diagnostics": "Application application_1492780173422_0003 failed 5 times due to ApplicationMaster for attempt appattempt_1492780173422_0003_000005 timed out. Failing the application.", "trackingUrl": "http://hn0-zhwe-s.uhunwunss5gupibv1jib3beicb.lx.internal.cloudapp.net:8088/cluster/app/application_1492780173422_0003", "id": "application_1492780173422_0003", "user": "livy", "name": "SparkCore_WasbIOTest", "queue": "default", "state": "ACCEPTED", "finalStatus": "ACCEPTED", "progress": 100, "trackingUI": "History", "elapsedTime": 2311292, "amContainerLogs": "http://10.0.0.15:30060/node/containerlogs/container_e03_1492780173422_0003_05_000001/livy", "amHostHttpAddress": "10.0.0.15:30060", "allocatedMB": -1, "allocatedVCores": -1, "runningContainers": -1, "memorySeconds": 3549035, "vcoreSeconds": 2308, "queueUsagePercentage": 0, "clusterUsagePercentage": 0, "preemptedResourceMB": 0, "preemptedResourceVCores": 0, "numNonAMContainerPreempted": 0, "numAMContainerPreempted": 0, "logAggregationStatus": "SUCCEEDED", "unmanagedApplication": false }}' with status code 200
    Then getting Spark driver host from URL '/batch', batch ID 9 should be '10.0.0.15'

  Scenario: getSparkDriverHost negative integration test with responses for failed jobs
    Given setup a mock livy service for GET request '/batch/9' to return '{"id":9,"state":"starting","appId":"application_1492415936046_0015","appInfo":{"driverLogUrl":"http://127.0.0.1:$port/yarnui/10.0.0.15/node/containerlogs/container_e02_1492415936046_0015_01_000001/livy","sparkUiUrl":"https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/"},"log":["\\t ApplicationMaster RPC port: -1","\\t queue: default","\\t start time: 1492569369011","\\t final status: UNDEFINED","\\t tracking URL: https://spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1492415936046_0015/","\\t user: livy","17/04/19 02:36:09 INFO ShutdownHookManager: Shutdown hook called","17/04/19 02:36:09 INFO ShutdownHookManager: Deleting directory /tmp/spark-1984dc9d-acd4-4648-9104-398431590f8e","YARN Diagnostics:","AM container is launched, waiting for AM container to Register with RM"]}' with status code 200
    And setup a mock livy service for GET request '/yarnui/ws/v1/cluster/apps/application_1492415936046_0015' to return '{ "app": { "amNodeLabelExpression": "", "finishedTime": 1493100184345, "startedTime": 1493097873053, "priority": 0, "applicationTags": "livy-batch-15-bcixsxv0", "applicationType": "SPARK", "clusterId": 1492780173422, "diagnostics": "Application application_1492780173422_0003 failed 5 times due to ApplicationMaster for attempt appattempt_1492780173422_0003_000005 timed out. Failing the application.", "trackingUrl": "http://hn0-zhwe-s.uhunwunss5gupibv1jib3beicb.lx.internal.cloudapp.net:8088/cluster/app/application_1492780173422_0003", "id": "application_1492780173422_0003", "user": "livy", "name": "SparkCore_WasbIOTest", "queue": "default", "state": "FAILED", "finalStatus": "FAILED", "progress": 100, "trackingUI": "History", "elapsedTime": 2311292, "amContainerLogs": "http://10.0.0.15:30060/node/containerlogs/container_e03_1492780173422_0003_05_000001/livy", "amHostHttpAddress": "10.0.0.15:30060", "allocatedMB": -1, "allocatedVCores": -1, "runningContainers": -1, "memorySeconds": 3549035, "vcoreSeconds": 2308, "queueUsagePercentage": 0, "clusterUsagePercentage": 0, "preemptedResourceMB": 0, "preemptedResourceVCores": 0, "numNonAMContainerPreempted": 0, "numAMContainerPreempted": 0, "logAggregationStatus": "SUCCEEDED", "unmanagedApplication": false }}' with status code 200
    Then getting Spark driver host from URL '/batch', batch ID 9 should be '__exception_got__'
    Then throw exception 'java.net.UnknownServiceException' with message 'The Livy job 9 on yarn is not running.'

  Scenario: getSparkJobYarnCurrentAppAttempt integration test with response
    Given setup a mock livy service for GET request '/yarnui/ws/v1/cluster/apps/application_1513565654634_0011/appattempts' to return '{"appAttempts":{"appAttempt":[{"id":1,"startTime":1513673984219,"finishedTime":0,"containerId":"container_1513565654634_0011_01_000001","nodeHttpAddress":"10.0.0.6:30060","nodeId":"10.0.0.6:30050","logsLink":"http://10.0.0.6:30060/node/containerlogs/container_1513565654634_0011_01_000001/livy","blacklistedNodes":"","appAttemptId":"appattempt_1513565654634_0011_000001"},{"id":2,"startTime":1513673985219,"finishedTime":0,"containerId":"container_1513565654634_0011_01_000002","nodeHttpAddress":"10.0.0.7:30060","nodeId":"10.0.0.7:30050","logsLink":"http://10.0.0.7:30060/node/containerlogs/container_1513565654634_0011_01_000002/livy","blacklistedNodes":"","appAttemptId":"appattempt_1513565654634_0011_000002"}]}}' with status code 200
   And mock method getSparkJobApplicationIdObservable to return 'application_1513565654634_0011' Observable
   Then getting current Yarn App attempt should be 'appattempt_1513565654634_0011_000002'

  Scenario: getSparkJobDriverLogUrlObservable unit test
    Given mock getSparkJobYarnCurrentAppAttempt with the following response:
      | logsLink | http://10.0.0.4:8042/node/containerlogs/container_1326821518301_0005_01_000001/user1 |
    And mock Spark job uri 'https://cluster/yarnui/10.0.0.4/node/containerlogs/container_1326821518301_0005_01_000001/user1' is valid
    And mock Spark job connect URI to be 'https://cluster/'
    Then getting Spark Job driver log URL Observable should be 'https://cluster/yarnui/10.0.0.4/node/containerlogs/container_1326821518301_0005_01_000001/user1'
    Given mock Spark job uri 'https://cluster/yarnui/10.0.0.4/node/containerlogs/container_1326821518301_0005_01_000001/user1' is invalid
    Given mock Spark job uri 'https://cluster/yarnui/10.0.0.4/port/8042/node/containerlogs/container_1326821518301_0005_01_000001/user1' is valid
    Then getting Spark Job driver log URL Observable should be 'https://cluster/yarnui/10.0.0.4/port/8042/node/containerlogs/container_1326821518301_0005_01_000001/user1'

  Scenario: getSparkJobDriverLogUrlObservable unit test for failure
    Given mock getSparkJobYarnCurrentAppAttempt with the following response:
      | logsLink | |
    And mock Spark job connect URI to be 'https://cluster/'
    Then getting Spark Job driver log URL Observable should be empty
