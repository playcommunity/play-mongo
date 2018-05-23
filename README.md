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


 

