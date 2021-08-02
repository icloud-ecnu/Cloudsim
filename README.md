# CloudSim+ #

本项目旨在CloudSim的基础上实现容器的扩容缩容，各层级的负载均衡，以制定用户侧长连接绑定延迟和服务商侧资源消耗的合理权衡方案。

要实现的目标主要涉及到Datacenter之间、Datacenter与Broker之间，Datacenter内部的数据交互，所以CloudSim Plus主体设计了两个继承类_（两者都在CloudSim/src/main/java/org/cloudbus/cloudsim/container/core中）_：

- __UserSideBroker__ (继承_ContainerDatacenterBroker_)
- __UserSideDatacenter__(继承_PowerContainerDatacenter_)

结果由java swing可视化输出。


