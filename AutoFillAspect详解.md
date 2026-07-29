# AutoFillAspect 详解

本文讲解 `com.sky.aspect.AutoFillAspect` 这个类的作用、执行流程，以及它背后涉及的几个基础概念：AOP、切面、切入点、通知、自定义注解、反射、ThreadLocal。

## 1. 这个类解决什么问题

在后台系统里，很多表都会有类似这样的公共字段：

```text
create_time   创建时间
create_user   创建人
update_time   修改时间
update_user   修改人
```

如果每次新增或修改数据时，都在 Service 里手动写：

```java
employee.setCreateTime(LocalDateTime.now());
employee.setCreateUser(currentUserId);
employee.setUpdateTime(LocalDateTime.now());
employee.setUpdateUser(currentUserId);
```

代码会重复很多。

`AutoFillAspect` 的目的就是：把这些公共字段的赋值逻辑抽出来，统一处理。

也就是说，业务代码只需要调用：

```java
employeeMapper.insert(employee);
```

而公共字段由 AOP 自动补上。

## 2. 涉及的几个类

这个功能不是单独靠 `AutoFillAspect` 完成的，而是几个类配合起来：

```text
AutoFill
自定义注解，用来标记某个 Mapper 方法需要自动填充字段。

OperationType
枚举，用来区分当前是 INSERT 还是 UPDATE。

AutoFillConstant
常量类，保存 setCreateTime、setUpdateTime 等方法名。

BaseContext
使用 ThreadLocal 保存当前登录用户 id。

AutoFillAspect
AOP 切面，真正执行自动填充逻辑。
```

## 3. AutoFill 注解

源码：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value();
}
```

它表示：这个注解只能加在方法上，并且运行时还能被读取到。

例如：

```java
@AutoFill(OperationType.INSERT)
void insert(Employee employee);
```

这句话的含义是：

```text
insert 方法执行前，需要按照 INSERT 类型自动填充公共字段。
```

再比如：

```java
@AutoFill(OperationType.UPDATE)
void update(Employee employee);
```

含义是：

```text
update 方法执行前，需要按照 UPDATE 类型自动填充公共字段。
```

注意：`@AutoFill` 本身只是一个标记，它不会自动执行任何逻辑。真正干活的是 `AutoFillAspect`。

## 4. OperationType 枚举

源码：

```java
public enum OperationType {
    UPDATE,
    INSERT
}
```

它用来告诉切面当前是什么数据库操作。

为什么要区分？

因为新增和修改要填充的字段不同。

新增时需要填充 4 个字段：

```text
createTime
createUser
updateTime
updateUser
```

修改时只需要填充 2 个字段：

```text
updateTime
updateUser
```

所以切面需要先知道当前操作类型，再决定调用哪些 setter 方法。

## 5. AutoFillAspect 完整结构

当前类大概结构是：

```java
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        ...
    }
}
```

可以先用一句话理解：

```text
当 com.sky.mapper 包下某个带有 @AutoFill 注解的方法即将执行时，
先进入 autoFill 方法，自动给参数对象设置公共字段。
```

## 6. @Aspect、@Component、@Slf4j

### @Aspect

```java
@Aspect
```

表示当前类是一个 AOP 切面类。

切面可以理解为：

```text
一段可以插入到其他方法执行前、执行后、异常时执行的公共逻辑。
```

比如：

```text
登录校验
日志记录
权限判断
事务处理
公共字段填充
```

这些逻辑都适合用 AOP 来做。

### @Component

```java
@Component
```

表示把这个类交给 Spring 容器管理。

如果没有这个注解，Spring 不会创建 `AutoFillAspect` 对象，AOP 也不会生效。

### @Slf4j

```java
@Slf4j
```

这是 Lombok 提供的日志注解。

加上它以后，可以直接写：

```java
log.info("开始进行公共字段自动填充...");
```

不需要自己手动创建 Logger。

## 7. 切入点 @Pointcut

源码：

```java
@Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
public void autoFillPointCut(){}
```

这个方法本身没有方法体逻辑，它只是给切入点表达式起了一个名字：

```java
autoFillPointCut()
```

后面的通知可以引用它。

重点看表达式：

```java
execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)
```

它由两部分组成。

### 7.1 execution 部分

```java
execution(* com.sky.mapper.*.*(..))
```

含义是：匹配 `com.sky.mapper` 包下所有类的所有方法。

拆开看：

```text
*                    返回值任意
com.sky.mapper       mapper 包
.*                   mapper 包下任意类
.*                   任意方法
(..)                 参数任意
```

所以它能匹配：

```java
com.sky.mapper.EmployeeMapper.insert(...)
com.sky.mapper.EmployeeMapper.update(...)
com.sky.mapper.CategoryMapper.insert(...)
```

但是它不会匹配 Service 层或 Controller 层的方法。

### 7.2 @annotation 部分

```java
@annotation(com.sky.annotation.AutoFill)
```

含义是：方法上必须有 `@AutoFill` 注解。

所以最终不是所有 Mapper 方法都会被拦截，而是必须同时满足两个条件：

```text
1. 在 com.sky.mapper 包下
2. 方法上有 @AutoFill 注解
```

例如这个会被拦截：

```java
@AutoFill(OperationType.INSERT)
void insert(Employee employee);
```

这个不会被拦截：

```java
Employee getByUsername(String username);
```

因为它没有 `@AutoFill`。

## 8. 前置通知 @Before

源码：

```java
@Before("autoFillPointCut()")
public void autoFill(JoinPoint joinPoint){
    ...
}
```

`@Before` 表示前置通知。

也就是：

```text
目标 Mapper 方法执行之前，先执行 autoFill 方法。
```

假设代码调用：

```java
employeeMapper.insert(employee);
```

实际执行顺序是：

```text
1. 进入 AutoFillAspect.autoFill(...)
2. 自动给 employee 设置 createTime、createUser、updateTime、updateUser
3. 再执行 EmployeeMapper.insert(employee)
4. MyBatis 把 employee 插入数据库
```

这就是为什么业务代码里可以少写很多公共字段赋值。

## 9. JoinPoint 是什么

源码：

```java
public void autoFill(JoinPoint joinPoint)
```

`JoinPoint` 可以理解为：

```text
当前被 AOP 拦截到的方法现场。
```

通过它可以拿到：

```text
当前调用的是哪个方法
这个方法有哪些注解
这个方法传了哪些参数
目标对象是谁
```

在这个项目里主要用它做两件事：

```text
1. 获取当前 Mapper 方法上的 @AutoFill 注解
2. 获取当前 Mapper 方法的参数，也就是实体对象
```

## 10. 获取 @AutoFill 注解里的操作类型

源码：

```java
MethodSignature signature = (MethodSignature) joinPoint.getSignature();
AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
OperationType operationType = autoFill.value();
```

逐句解释。

### 10.1 获取方法签名

```java
MethodSignature signature = (MethodSignature) joinPoint.getSignature();
```

`joinPoint.getSignature()` 可以拿到当前被拦截方法的签名信息。

方法签名可以简单理解为：

```text
方法的描述信息，包括方法名、参数、返回值等。
```

这里强转成 `MethodSignature`，是因为我们需要拿到真正的 `Method` 对象。

### 10.2 获取方法上的注解

```java
AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
```

这句是在读取目标 Mapper 方法上的注解。

假设 Mapper 方法是：

```java
@AutoFill(OperationType.INSERT)
void insert(Employee employee);
```

那么这里拿到的 `autoFill` 就代表这个注解对象。

### 10.3 获取注解里的 value

```java
OperationType operationType = autoFill.value();
```

如果注解是：

```java
@AutoFill(OperationType.INSERT)
```

那么：

```java
operationType == OperationType.INSERT
```

如果注解是：

```java
@AutoFill(OperationType.UPDATE)
```

那么：

```java
operationType == OperationType.UPDATE
```

## 11. 获取 Mapper 方法参数

源码：

```java
Object[] args = joinPoint.getArgs();
if(args == null || args.length == 0){
    return;
}

Object entity = args[0];
```

`joinPoint.getArgs()` 用来获取当前方法传入的所有参数。

比如 Mapper 方法是：

```java
void insert(Employee employee);
```

调用时：

```java
employeeMapper.insert(employee);
```

那么：

```java
args[0] == employee
```

所以：

```java
Object entity = args[0];
```

就是拿到当前要插入或修改的实体对象。

为什么类型是 `Object`？

因为这个切面不只服务于 `Employee`，也可以服务于其他实体类，比如 `Category`、`Dish`、`Setmeal`。

这些类的共同点是都有公共字段：

```text
createTime
createUser
updateTime
updateUser
```

所以这里不能写死成：

```java
Employee entity = ...
```

而是用更通用的：

```java
Object entity = args[0];
```

## 12. 准备要填充的数据

源码：

```java
LocalDateTime now = LocalDateTime.now();
Long currentId = BaseContext.getCurrentId();
```

这里准备了两个值。

### 12.1 当前时间

```java
LocalDateTime now = LocalDateTime.now();
```

用于填充：

```text
createTime
updateTime
```

### 12.2 当前登录用户 id

```java
Long currentId = BaseContext.getCurrentId();
```

用于填充：

```text
createUser
updateUser
```

`BaseContext` 内部用的是 `ThreadLocal`：

```java
public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
```

它的作用是：在当前请求线程中保存当前登录用户 id。

通常流程是：

```text
1. 前端请求带着 token
2. 拦截器解析 token，拿到 userId
3. 拦截器调用 BaseContext.setCurrentId(userId)
4. 后续 Service、Mapper、Aspect 都可以通过 BaseContext.getCurrentId() 拿到当前用户 id
```

所以切面里就能知道是谁在新增或修改数据。

## 13. 反射是什么

这个类里最重要、也最容易卡住的部分是反射。

源码里有这种代码：

```java
Method setCreateTime = entity.getClass().getDeclaredMethod(
        AutoFillConstant.SET_CREATE_TIME,
        LocalDateTime.class
);
```

反射可以理解为：

```text
程序运行时，动态获取类的信息，并动态调用它的方法。
```

平时我们调用 setter 是这样：

```java
employee.setCreateTime(now);
```

这是写死的。

但是在切面里，`entity` 可能是 `Employee`，也可能是 `Category`，也可能是其他实体。

所以不能直接写：

```java
entity.setCreateTime(now);
```

因为 `entity` 的编译类型是 `Object`，`Object` 类没有 `setCreateTime` 方法。

于是用反射：

```java
Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
setCreateTime.invoke(entity, now);
```

意思是：

```text
1. 看看 entity 实际是哪个类
2. 在这个类里找 setCreateTime(LocalDateTime) 方法
3. 找到后调用它，把 now 传进去
```

## 14. AutoFillConstant 的作用

源码：

```java
public class AutoFillConstant {
    public static final String SET_CREATE_TIME = "setCreateTime";
    public static final String SET_UPDATE_TIME = "setUpdateTime";
    public static final String SET_CREATE_USER = "setCreateUser";
    public static final String SET_UPDATE_USER = "setUpdateUser";
}
```

这些常量是给反射用的。

如果不用常量，代码里会写很多字符串：

```java
getDeclaredMethod("setCreateTime", LocalDateTime.class);
getDeclaredMethod("setCreateUser", Long.class);
getDeclaredMethod("setUpdateTime", LocalDateTime.class);
getDeclaredMethod("setUpdateUser", Long.class);
```

使用常量后：

```java
getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
```

好处是：

```text
1. 避免字符串写错
2. 统一管理方法名
3. 后续要改名字时只改常量类
```

## 15. INSERT 分支

源码：

```java
if(operationType == OperationType.INSERT){
    try {
        Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
        Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
        Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
        Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

        setCreateTime.invoke(entity,now);
        setCreateUser.invoke(entity,currentId);
        setUpdateTime.invoke(entity,now);
        setUpdateUser.invoke(entity,currentId);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

当 Mapper 方法上写的是：

```java
@AutoFill(OperationType.INSERT)
```

就会进入这个分支。

它做了两步：

```text
1. 获取实体类中的 4 个 setter 方法
2. 调用这 4 个 setter 方法赋值
```

等价于手写：

```java
entity.setCreateTime(now);
entity.setCreateUser(currentId);
entity.setUpdateTime(now);
entity.setUpdateUser(currentId);
```

只是这里用反射实现，能兼容不同实体类。

## 16. UPDATE 分支

源码：

```java
else if(operationType == OperationType.UPDATE){
    try {
        Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
        Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

        setUpdateTime.invoke(entity,now);
        setUpdateUser.invoke(entity,currentId);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

当 Mapper 方法上写的是：

```java
@AutoFill(OperationType.UPDATE)
```

就会进入这个分支。

它只填充：

```text
updateTime
updateUser
```

因为修改数据时不应该改 `createTime` 和 `createUser`。

## 17. 完整执行流程

以新增员工为例，假设 Mapper 方法是：

```java
@AutoFill(OperationType.INSERT)
void insert(Employee employee);
```

Service 调用：

```java
employeeMapper.insert(employee);
```

执行流程如下：

```text
1. Service 调用 employeeMapper.insert(employee)
2. Spring AOP 发现 insert 方法匹配切入点
3. 因为是 @Before，所以先执行 AutoFillAspect.autoFill(joinPoint)
4. 切面通过 joinPoint 获取当前方法
5. 切面读取方法上的 @AutoFill 注解
6. 从注解中拿到 OperationType.INSERT
7. 切面通过 joinPoint.getArgs() 获取 employee 参数
8. 切面准备 now 和 currentId
9. 切面通过反射调用 employee 的 setCreateTime、setCreateUser、setUpdateTime、setUpdateUser
10. 公共字段填充完成
11. 原来的 employeeMapper.insert(employee) 继续执行
12. MyBatis 将带有公共字段的 employee 插入数据库
```

修改数据时流程类似，只是注解值是 `OperationType.UPDATE`，只填充 `updateTime` 和 `updateUser`。

## 18. 为什么放在 Mapper 层

切入点写的是：

```java
execution(* com.sky.mapper.*.*(..))
```

说明它拦截的是 Mapper 方法。

这样做的好处是：

```text
1. 离数据库操作最近
2. 不管哪个 Service 调用 Mapper，都能统一填充
3. 避免每个 Service 方法都重复写公共字段
```

但也有一个前提：

```text
Mapper 方法的第一个参数必须是需要填充的实体对象。
```

因为代码里写的是：

```java
Object entity = args[0];
```

如果某个 Mapper 方法第一个参数不是实体对象，或者实体类没有对应 setter 方法，反射就会失败。

## 19. 这个切面对实体类有什么要求

被自动填充的实体类必须有这些方法：

```java
setCreateTime(LocalDateTime createTime)
setCreateUser(Long createUser)
setUpdateTime(LocalDateTime updateTime)
setUpdateUser(Long updateUser)
```

对于 UPDATE 至少要有：

```java
setUpdateTime(LocalDateTime updateTime)
setUpdateUser(Long updateUser)
```

如果实体类用了 Lombok：

```java
@Data
public class Employee {
    private LocalDateTime createTime;
    private Long createUser;
    private LocalDateTime updateTime;
    private Long updateUser;
}
```

那么 Lombok 会自动生成这些 setter 方法。

## 20. 和以前手动 set 的区别

以前写法：

```java
employee.setCreateTime(LocalDateTime.now());
employee.setUpdateTime(LocalDateTime.now());
employee.setCreateUser(10L);
employee.setUpdateUser(10L);
employeeMapper.insert(employee);
```

现在写法：

```java
employeeMapper.insert(employee);
```

前提是 Mapper 方法上加：

```java
@AutoFill(OperationType.INSERT)
```

公共字段由切面自动补齐。

## 21. BaseContext 和 ThreadLocal 简单复习

`BaseContext`：

```java
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
```

`ThreadLocal` 可以理解为：

```text
给每个线程单独准备一份变量副本。
```

Web 项目中，一个请求通常由一个线程处理。

所以当拦截器解析 token 后调用：

```java
BaseContext.setCurrentId(userId);
```

当前请求后续执行到 Service、Mapper、Aspect 时，都可以拿到同一个 `userId`：

```java
BaseContext.getCurrentId();
```

这样切面就能知道当前操作人是谁。

注意：请求结束后最好调用：

```java
BaseContext.removeCurrentId();
```

避免线程复用时残留旧用户 id。

## 22. AOP 基础概念复习

### 22.1 AOP 是什么

AOP 全称是 Aspect Oriented Programming，面向切面编程。

它适合处理横向公共逻辑。

所谓横向公共逻辑，就是很多业务都会用到，但又不属于某一个具体业务本身的逻辑。

例如：

```text
日志
权限
事务
异常处理
公共字段填充
```

### 22.2 切面 Aspect

切面就是公共逻辑所在的类。

在这个项目里：

```java
AutoFillAspect
```

就是切面。

### 22.3 切入点 Pointcut

切入点用来定义：

```text
哪些方法会被 AOP 拦截。
```

本项目的切入点是：

```java
@Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
```

意思是：

```text
拦截 Mapper 包下带 @AutoFill 注解的方法。
```

### 22.4 通知 Advice

通知用来定义：

```text
什么时候执行切面逻辑。
```

常见通知类型：

```text
@Before        目标方法执行前
@After         目标方法执行后，不管成功还是异常
@AfterReturning 目标方法成功返回后
@AfterThrowing 目标方法抛异常后
@Around        环绕通知，可以控制目标方法是否执行
```

本项目用的是：

```java
@Before
```

因为公共字段必须在插入或修改数据库之前设置好。

### 22.5 连接点 JoinPoint

连接点可以理解为：

```text
被拦截到的那个方法执行现场。
```

本项目用它来获取：

```text
方法信息
方法注解
方法参数
```

## 23. 为什么这里不用普通 setter

因为切面希望适配多个实体类。

如果只服务 `Employee`，可以写：

```java
Employee employee = (Employee) args[0];
employee.setCreateTime(now);
```

但这个切面是通用的，可能要服务：

```text
Employee
Category
Dish
Setmeal
```

所以它把参数当成 `Object`：

```java
Object entity = args[0];
```

然后通过反射调用对应方法。

这就是反射在这里的价值：

```text
不关心具体实体类类型，只要它有对应 setter，就可以统一赋值。
```

## 24. 这段代码的潜在问题

当前代码里异常处理是：

```java
catch (Exception e) {
    e.printStackTrace();
}
```

学习阶段可以这样写，方便看错误。

但在正式项目里一般不推荐只打印异常，因为如果自动填充失败，程序可能还会继续执行，导致数据库字段为空。

更严谨的做法通常是：

```java
log.error("公共字段自动填充失败", e);
throw new RuntimeException("公共字段自动填充失败");
```

这样问题会更早暴露。

另外还要注意：

```text
1. Mapper 方法第一个参数必须是实体对象
2. 实体类必须有对应 setter
3. BaseContext 中必须已经保存了当前用户 id
4. @AutoFill 必须加在 Mapper 方法上
```

任何一个条件不满足，都可能导致自动填充不生效。

## 25. 一句话总结

`AutoFillAspect` 是一个 AOP 切面，它会在带有 `@AutoFill` 注解的 Mapper 方法执行前触发，根据注解中的 `OperationType` 判断是新增还是修改，然后通过反射给实体对象自动设置创建时间、创建人、修改时间、修改人等公共字段。

可以把它记成：

```text
@AutoFill 负责标记
OperationType 负责说明操作类型
AutoFillAspect 负责拦截并填字段
BaseContext 负责提供当前用户 id
反射负责动态调用实体对象的 setter 方法
```
