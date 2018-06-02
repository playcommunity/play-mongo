# Introduction
play-mongo is a play module focusing on the integration of [official mongodb driver](https://github.com/mongodb/mongo-java-driver) with [play scala framework](https://github.com/playframework/playframework).
It's designed based on [official mongodb scala driver](https://github.com/mongodb/mongo-scala-driver) and bring lots of features designed for play framework.

- take the stability of official mongodb driver
- auto generate the codecs of user's models
- dsl for building queries
- relation mapping

## Create
```
mongo.insert[Order](Order("0", Nil))
```

## Update
```
mongo.update[Order](Json.obj("_id" -> "0"), Json.obj("$set" -> ...))
```

## Query
```
mongo.find[Order](Json.obj("_id" -> "0")).first
```

## Delete
```
mongo.remove[Order](Json.obj("_id" -> "0"))
```

## Useful
```
mongo.database
mongo.collection("colName")
```

## 如何处理json转bson的数值类型丢失问题？
MongoDB的Bson在Json的基础上新增了更丰富的子类型，导致在转换时会出现类型丢失问题。 `play-mongo`经常用到以下几种转换，我们逐个分析。
```
case class User(_id: String, name: String, setting: UserSetting)
case class UserSetting(gender: String, age: Int)
```
### Class -> Bson
```
mongo.insert[User].insert(User("0", "joymufeng", UserSetting("male", 32)))
```
在调用底层驱动的插入操作之前，需要先将`User`转换成BsonDocument。通常我们只会在User类中定义MongoDB可以存储的数据类型，所以这个转换过程没有类型丢失问题。

### Json -> Bson
Json只有一个数值类型JsNumber，内部使用BigDecimal存储数据。所以在使用Json DSL 构建Json对象时，所有的数值类型(Byte, Short, Int, Long, Float, Double)均会被转换
成BigDecimal类型。所以`Json -> Bson`的转换过程存在类型丢失问题。

### Class -> Json -> Bson 
```
mongo.update[User](Json.obj("_id" -> "0"), Json.obj("$set" -> UserSetting("male", 18)))
```
`UserSetting("male", 32)`会被先转换成`Json.obj("gender" -> JsString("male"), "age" -> JsNumber(BigDecimal(18))`, 同上，UserSetting中的所有数据类型均会被转换成BigDecimal类型。

问题的根源在于Json只包含一种数值类型JsNumber，并且会将所有的数值类型转换成BigDecimal，这插入和更新时没问题，但是在读取时就遇到麻烦了。例如，我们这样更新用户设置：
```
mongo.update[User](Json.obj("_id" -> "0"), Json.obj("$set" -> UserSetting("male", 18)))
//或者
mongo.update[User](Json.obj("_id" -> "0"), Json.obj("$set" -> Json.obj("setting" -> Json.obj("gender" -> "male", "age" -> 18))))
```
上面的更新操作可以成功执行，但是数据库中该用户的`setting.age`类型变成了NumberDecimal类型，当查询该用户时便会发生类型转换错误：
```
mongo.find[User](Json.obj("_id" -> "0")).first() 
// [BsonInvalidOperationException: Invalid numeric type, found: DECIMAL128]
```
在尝试将NumberDecimal转换成Int时发生了精度丢失，所以抛出了类型转换错误。

为了解决这个问题，`play-mongo`在使用Json进行插入或更新操作时会将JsNumber类型尽可能地转换为较窄的类型，使得在读取时不会发生数值精度丢失问题。例如针对上面的两个更新
操作，`play-mongo`会自动将`18`转换为Int类型并执行更新操作。最终在数据库中该用户的`setting.age`类型为BsonInt32， 即使UserSetting的age属性为Double类型，也可以成功执行读取操作，`play-mongo`
会自动将BsonInt32转换为Double类型，因为是从窄类型向宽类型转换，所以不会发生精度丢失问题。

## 文件上传和下载
```
mongo.gridFSBucket.uploadFromFile("kf.jpg", "image/jpg", new File("./kf.jpg")).map{ fileId =>
  Ok(fileId)
}

mongo.gridFSBucket.findById("5b1183fed3ba643a3826325f").map{
  case Some(file) =>
    Ok.chunked(file.stream.toSource)
      .as(file.getContentType)
  case None =>
    NotFound
}
```
