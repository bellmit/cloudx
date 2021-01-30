#### MVC框架
- 对MVC框架接口的统一模块封装
- 支持Action等各种服务注解
- 插件化管理
- 支持请求拦截器自定义
- 支持请求监听
- 支持参数解析
- 支持客户端负载均衡和故障转移

### 开发计划
- 开发qps监听服务 √
- 开发handler可以匹配所有的method √
- 开发handler可以匹配指定host √
- 开发支持redis/codis/jvm等的组件加载和配置
- 开发Action实例的资源如何释放，通过beanfactory的destroy将实现了AutoClosable接口的单例实例进行资源释放 √
- 开发支持RxIo响应式异步输出 √
- 开发支持okhttp注解直接使用 √
- 开发okhttp传递流水号