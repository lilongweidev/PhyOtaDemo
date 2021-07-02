# phyota
PHY Bluetooth Low Energy OTA Library

## 使用步骤

### 一、添加依赖

Step 1. 项目工程的build.gradle，添加jitpack库

```groovy
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```
  
Step 2. 模块的build.gradle，添加ota依赖库

```groovy
dependencies {
	implementation 'com.github.lilongweidev:phyota:1.0.4'
}
```

### 二、功能介绍

库中提供了蓝牙ble扫描和连接，文件解析，ota升级。


