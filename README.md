# CloudSim+ #

本项目旨在CloudSim的基础上实现容器的扩容缩容，各层级的负载均衡，以制定用户侧长连接绑定延迟和服务商侧资源消耗的合理权衡方案。

要实现的目标主要涉及到Datacenter之间、Datacenter与Broker之间，Datacenter内部的数据交互，所以CloudSim Plus主体设计了两个继承类_（两者都在CloudSim/src/main/java/org/cloudbus/cloudsim/container/core中）_：

- __UserSideBroker__ (继承_ContainerDatacenterBroker_)
- __UserSideDatacenter__(继承_PowerContainerDatacenter_)

结果由java swing可视化输出。

#### 运行方式：

执行文件：Cloudsim\modules\cloudsim-examples\src\main\java\org\cloudbus\cloudsim\examples\container\PredictationTest.java

note:
1. 首次执行需要将[Line 162](https://github.com/icloud-ecnu/Cloudsim/blob/7820db01348075bdf762a6b7617bccf40cf63374/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/container/PredictationTest.java#L162) 注释关掉，因为该行是生成输入数据的函数。
2. 取消注释并执行过后会产生将本次模拟的输入数据以txt方式输出便于后续对比分析，后续可以自行选择是否重新生成输入数据。
3. 可视化框中的结果一栏，balance factor对比需要三种load balance strategy均运行一遍之后才可以产生，即将[Line 168](https://github.com/icloud-ecnu/Cloudsim/blob/7820db01348075bdf762a6b7617bccf40cf63374/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/container/PredictationTest.java#L168)的*CloudSim.LoadBalanceStrategy*分别赋值为 “0”,“1”,“2”运行一遍。注意：若要对比，三组实验要采用统一输入数据。
