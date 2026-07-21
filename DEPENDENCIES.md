# CarePilot Server — 依赖版本清单

> 生成时间：2026-07-17  |  Spring Boot 3.3.5  |  Java 21

## 直接依赖（pom.xml 声明）

| GroupId | ArtifactId | 版本 |
|---------|-----------|------|
| org.springframework.boot | spring-boot-starter-actuator | 3.3.5 |
| org.springframework.boot | spring-boot-starter-web | 3.3.5 |
| org.springframework.boot | spring-boot-starter-validation | 3.3.5 |
| com.baomidou | mybatis-plus-spring-boot3-starter | 3.5.9 |
| com.baomidou | mybatis-plus-jsqlparser-4.9 | 3.5.9 |
| com.mysql | mysql-connector-j | 8.3.0 |
| org.projectlombok | lombok | 1.18.34 |
| org.springframework.boot | spring-boot-starter-test | 3.3.5 (test) |

## 完整依赖树

### Spring Boot 核心
| ArtifactId | 版本 |
|-----------|------|
| spring-boot | 3.3.5 |
| spring-boot-autoconfigure | 3.3.5 |
| spring-boot-starter-logging | 3.3.5 |
| spring-boot-starter-jdbc | 3.3.5 |
| spring-boot-starter-json | 3.3.5 |
| spring-boot-starter-tomcat | 3.3.5 |
| spring-boot-actuator | 3.3.5 |
| spring-boot-actuator-autoconfigure | 3.3.5 |

### Spring Framework
| ArtifactId | 版本 |
|-----------|------|
| spring-aop | 6.1.14 |
| spring-beans | 6.1.14 |
| spring-context | 6.1.14 |
| spring-core | 6.1.14 |
| spring-expression | 6.1.14 |
| spring-jcl | 6.1.14 |
| spring-jdbc | 6.1.14 |
| spring-tx | 6.1.14 |
| spring-web | 6.1.14 |
| spring-webmvc | 6.1.14 |

### 日志
| ArtifactId | 版本 |
|-----------|------|
| logback-classic | 1.5.11 |
| logback-core | 1.5.11 |
| log4j-api | 2.23.1 |
| log4j-to-slf4j | 2.23.1 |
| jul-to-slf4j | 2.0.16 |
| slf4j-api | 2.0.16 |

### Jackson（JSON）
| ArtifactId | 版本 |
|-----------|------|
| jackson-core | 2.17.2 |
| jackson-annotations | 2.17.2 |
| jackson-databind | 2.17.2 |
| jackson-datatype-jdk8 | 2.17.2 |
| jackson-datatype-jsr310 | 2.17.2 |
| jackson-module-parameter-names | 2.17.2 |

### Tomcat（嵌入式）
| ArtifactId | 版本 |
|-----------|------|
| tomcat-embed-core | 10.1.31 |
| tomcat-embed-el | 10.1.31 |
| tomcat-embed-websocket | 10.1.31 |

### MyBatis-Plus / 数据库
| ArtifactId | 版本 |
|-----------|------|
| mybatis-plus | 3.5.9 |
| mybatis-plus-core | 3.5.9 |
| mybatis-plus-annotation | 3.5.9 |
| mybatis-plus-extension | 3.5.9 |
| mybatis-plus-spring | 3.5.9 |
| mybatis-plus-spring-boot-autoconfigure | 3.5.9 |
| mybatis-plus-jsqlparser-common | 3.5.9 |
| mybatis | 3.5.16 |
| mybatis-spring | 3.0.4 |
| jsqlparser | 4.9 |
| mysql-connector-j | 8.3.0 |
| HikariCP | 5.1.0 |

### 参数校验
| ArtifactId | 版本 |
|-----------|------|
| hibernate-validator | 8.0.1.Final |
| jakarta.validation-api | 3.0.2 |
| classmate | 1.7.0 |

### Micrometer（监控指标）
| ArtifactId | 版本 |
|-----------|------|
| micrometer-core | 1.13.6 |
| micrometer-commons | 1.13.6 |
| micrometer-observation | 1.13.6 |
| micrometer-jakarta9 | 1.13.6 |

### 测试
| ArtifactId | 版本 |
|-----------|------|
| spring-boot-test | 3.3.5 |
| spring-boot-test-autoconfigure | 3.3.5 |
| spring-test | 6.1.14 |
| junit-jupiter | 5.10.5 |
| junit-jupiter-api | 5.10.5 |
| junit-jupiter-engine | 5.10.5 |
| junit-jupiter-params | 5.10.5 |
| junit-platform-commons | 1.10.5 |
| junit-platform-engine | 1.10.5 |
| mockito-core | 5.11.0 |
| mockito-junit-jupiter | 5.11.0 |
| assertj-core | 3.25.3 |
| hamcrest | 2.2 |
| json-path | 2.9.0 |
| json-smart | 2.5.1 |
| jsonassert | 1.5.3 |
| xmlunit-core | 2.9.1 |
| awaitility | 4.2.2 |
| byte-buddy | 1.14.19 |
| byte-buddy-agent | 1.14.19 |
| objenesis | 3.3 |
| opentest4j | 1.3.0 |

### 其他
| ArtifactId | 版本 |
|-----------|------|
| lombok | 1.18.34 |
| snakeyaml | 2.2 |
| jakarta.annotation-api | 2.1.1 |
| HdrHistogram | 2.2.2 |
| LatencyUtils | 2.0.3 |
| jboss-logging | 3.5.3.Final |
