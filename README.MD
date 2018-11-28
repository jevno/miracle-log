## 原理及背景介绍
  miracle-log对当下流行的日志框架进行了调研比较，最终确定使用log4j2，并在此基础上进行展实现。主要因有以下两点：
1. log4j2性能及易用性方面都对log4j进行了很大的改进与logback不相上下；
2. 我们原先大部分框架都默认使用了log4j,log4j2提供了对log4j的兼容实现包，这样框架版本更新过程中更容易平滑过渡。

## miracle-log扩展支持的新feature
- 新增CatAppender,默认对接CAT，ERROR级别日志直接上发CAT监控系统
- 新增MqBusinessAppender，业务关键指标埋点直接通过MQ上发到大数据中心
- 新增MsgIdPatternConverter，业务日志打印过程中，通过日志格式配置y%，增加该跳日志对应的CAT消息数的MsgId，可以实现日志跟CAT的精准对接

## miracle-log接入方式

### 普通jar包接入
当前可用版本为1.0.2

```
<dependency>
    <groupId>com.miracle.common</groupId>
    <artifactId>miracle-log</artifactId>
    <version>1.0.4</version>
</dependency>
```
ps:这里注意由于很多框架包及第三方包会间接引入log4j，这样会导致jar包冲突，从而引起日志打印的混乱，故这里需要把所有log4j依赖exclude掉。我们这里提供一种解决方案是为每个要exclude的jar构建一个虚假的ghost版本，然后将这个ghost版本依赖添加到当前项目中或者parent pom中，这样根据maven依赖的最短路径优先原则，ghost版本会默认生效，但其本身没有任何实现，从而间接达到exclude掉jar包的目的。如下所示：


```
<ghost.version>1.0.0-FAKE</ghost.version>
<dependency>
	<artifactId>log4j</artifactId>
	<groupId>log4j</groupId>
	<version>${ghost.version}</version>
</dependency>
<dependency>
	<groupId>org.slf4j</groupId>
	<artifactId>slf4j-log4j12</artifactId>
	<version>${ghost.version}</version>
</dependency>
```


### springboot版本管理器接入
如果使用springboot框架的话，直接使用我们提供的框架版本控制器1.2.1版本以上即可，项目中再包含一个无版本的miracle-log依赖即可

```

<dependency>
    <groupId>com.miracle.common</groupId>
    <artifactId>miracle-log</artifactId>
</dependency>

<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>com.miracle</groupId>
				<artifactId>miracle-common-dependencies</artifactId>
				<version>${miracle.common.dependencies.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
			<dependency>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-dependencies</artifactId>
				<version>${springboot.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

```

## miracle-log配置文件示例
miracle-log配置兼容log4j2的配置，只需注意我们扩展的几个新feature配置即可，如CatAppender，MqBusinessAppender， %y
log4j2.xml 样例如下：

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<!-- Log4j 2.x 配置文件。每30秒自动检查和应用配置文件的更新； -->
<Configuration status="warn" monitorInterval="30" strict="true" schema="Log4J-V2.2.xsd">
    <Appenders>
        <!-- 输出到控制台 -->
        <Console name="Console" target="SYSTEM_OUT">
            <!-- 需要记录的级别 -->
            <!-- <ThresholdFilter level="debug" onMatch="ACCEPT" onMismatch="DENY" /> -->
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS}:%4p %t (%F:%L) - %m%n" />
        </Console>

        <!-- 输出到文件，按天或者超过80MB分割 -->
        <RollingFile name="RollingFile" fileName="logs/log.log"  filePattern="logs/$${date:yyyy-MM}/log-%d{yyyy-MM-dd}-%i.log.gz">
            <!-- 需要记录的级别 -->
            <!-- <ThresholdFilter level="info" onMatch="ACCEPT" onMismatch="DENY" /> -->
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss,SSS}:%4p %t (%F:%L) %y - %m%n" />
            <Policies>
                <OnStartupTriggeringPolicy />
                <TimeBasedTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="80 MB" />
            </Policies>
        </RollingFile>

        <CatAppender name="CatErr">
        </CatAppender>

        <MqBusinessAppender name="MqBusiness" mqConfFile="kkhistory.properties" avroConfFile="user.avsc">
        </MqBusinessAppender>
        
        <!-- MQ上发业务埋点启用异步发送机制 -->
        <Async name="AsyncMqBusiness">
            <AppenderRef ref="MqBusiness"/>
        </Async>
    </Appenders>
    <Loggers>
        <Root level="info"> <!-- 全局配置 -->
            <AppenderRef ref="Console" />
            <AppenderRef ref="RollingFile"/>
        </Root>
        <Logger name="com.melot" level="info" additivity="false">
            <AppenderRef ref="RollingFile"/>
            <AppenderRef ref="CatErr"/>
            <AppenderRef ref="AsyncMqBusiness"/>
        </Logger>
        <!-- 为sql语句配置特殊的Log级别，方便调试 -->

        <Logger name="RocketmqClient" level="WARN" additivity="false">
            <AppenderRef ref="Console" />
        </Logger>
        <Logger name="RocketmqRemoting" level="WARN" additivity="false">
            <AppenderRef ref="Console" />
        </Logger>

    </Loggers>

```
## miracle-log程序中应用
### 创建新的Logger

```
MiracleLogger logger = MiracleLogger.getLogger(Main.class);
```
ps：如不需要业务指标埋点功能，那么可以直接使用log4j2默认方法或slf4j来创建logger

### 业务日志打印
与log4j2没有任何不同

```
log.info("hello {}", "wqj");
log.error("test-nothrow");
log.error("test", new RuntimeException("mock runtimeexception"));
```
### 业务指标数据埋点方法
数据埋点方法提供了三种实现
第一个参数avroType指定指标类型，key以"key1={},key2={}"格式置于messge中对应的value值依次置于后面的参数列表中
```
public void business(String avroType, final String message, final Object... params) 
```
2.key和value都分别以数组形式传入

```
 public void business(String avroType, String[] keysArray, Object[] paramsArray)
```
3.key value以Map形式传入

```
public void business(String avroType, Map<String, Object> kvMap)
```
### 应用实例
  avro埋点格式配置如下：
  
```
[{"type": "record",
 "name": "user",
 "namespace" : "avro",
 "fields": [
     {"name": "name", "type": "string"},
     {"name": "favoriteNumber",  "type": ["int", "null"]},
     {"name": "favoriteColor", "type": ["string", "null"]}
 ]
},
{"type": "record",
 "name": "product",
 "namespace" : "avro",
 "fields": [
         {"name": "productSeries", "type": "string"},
         {"name": "productName", "type": "string"},
         {"name": "tags", "type": "string"},
         {"name":"desc_model","type":"string"},
         {"name":"desc_price","type":"float"}
     ]
}]
```
埋点代码如下：

```
MiracleLogger logger = MiracleLogger.getLogger(Main.class);

logger.business("product",
        "productSeries={},productName={},tags={},desc_model={},desc_price={}",
        "aaa", "bbb", new String[]{"aa", "bb"}, "model", 20.0f);

ProductModel model1 = new ProductModel();
model1.setModel("xmodel");
model1.setPrice(30.0f);
logger.business("product",new String[]{"productSeries", "productName", "tags", "desc"},
        new Object[]{"ccc", "ddd", new String[]{"aa", "bb"}, model1});

Map<String, Object> modelAttrs = Maps.newHashMap();
modelAttrs.put("model", "ymodel");
modelAttrs.put("price", 100.0f);
logger.business("product",new String[]{"productSeries", "productName", "tags", "desc"},
        new Object[]{"ccc", "ddd", new String[]{"aa", "bb"}, modelAttrs});
```










