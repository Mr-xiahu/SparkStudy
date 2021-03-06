package cn.xhjava.spark.sql

import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{DataType, DoubleType, LongType, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}

/**
  * 用户自定义聚合函数(弱类型)
  */
object SparkSQL_04_UDAF {
  def main(args: Array[String]): Unit = {
    //初始化SparkSession
    val spark: SparkSession = SparkSession.builder()
      .appName("SparkSQL_04_UDAF")
      .config("spark.some.config.option", "some-value")
      .master("local[3]")
      .getOrCreate()

    //自定义聚合函数
    val udaf = new MyAgeAvgFunction
    spark.udf.register("avgAge", udaf)

    val peopleDF: DataFrame = spark.read.json("in/people.json")

    peopleDF.createOrReplaceTempView("user")
    spark.sql("select avgAge(age) from user").show()


    //释放资源
    spark.stop()
  }

  //增加一个样例类
  case class User(id: Int, name: String, age: Int)

}

//申明用户自定义聚合函数(弱类型)
//1.继承UserDefinedAggregateFunction类
//2.实现其方法

class MyAgeAvgFunction extends UserDefinedAggregateFunction {

  //函数输入的数据结构
  override def inputSchema: StructType = {
    new StructType().add("age", LongType)
  }

  //计算时的数据结构
  override def bufferSchema: StructType = {
    new StructType().add("sum", LongType).add("count", LongType)
  }

  //函数返回的数据类型
  override def dataType: DataType = DoubleType

  //函数是否稳定
  override def deterministic: Boolean = true

  //计算之前缓冲区初始化
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = 0l
    buffer(1) = 0l
  }


  //根据查询结果更新缓冲区数据
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    buffer(0) = buffer.getLong(0) + input.getLong(0)
    buffer(1) = buffer.getLong(1) + 1
  }

  //将多个节点的缓冲区合并
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    //sum
    buffer1(0) = buffer1.getLong(0) + buffer2.getLong(0)
    //count
    buffer1(1) = buffer1.getLong(1) + buffer2.getLong(1)
  }

  //计算
  override def evaluate(buffer: Row): Any = {
    buffer.getLong(0) / buffer.getLong(1).toDouble
  }
}


