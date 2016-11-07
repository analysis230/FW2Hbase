package com.whiteklay

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.Row

import java.io.Serializable

import org.apache.spark.sql.functions.monotonicallyIncreasingId
import org.apache.hadoop.fs._
import org.apache.hadoop.conf._
import org.apache.hadoop.mapred.TextInputFormat
import org.apache.phoenix.spark._
import com.databricks.spark.csv

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

import org.apache.hadoop.hbase.{HBaseConfiguration, HTableDescriptor}
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.mapred.TableOutputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.hbase.KeyValue
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles

object FW2Phoenix_UniqueId extends Serializable{

	case class logTable(rectype: String, imsi_number: String, subno: String, int_flag: String, imei_number: String, b_subno: String, transdate: String, act_duration: String, lac: String, cell_id: String, switch: String, call_type: String, third_party_no: String, category: String, prepost_flag: String, route_out: String, route_in: String, file_name: String)
	case class logTableID(ID: String, subno: String, imei_number: String, b_subno: String)
	case class fileDetails(filename: String, Status: Int)

 def main(args: Array[String]): Unit = {
		
		val conf = HBaseConfiguration.create()
		val tableName = "hao"
		val table = new HTable(conf, tableName) 
		 
		conf.set(TableOutputFormat.OUTPUT_TABLE, tableName)
		val job = Job.getInstance(conf)
		job.setMapOutputKeyClass (classOf[ImmutableBytesWritable])
		job.setMapOutputValueClass (classOf[KeyValue])
		HFileOutputFormat.configureIncrementalLoad (job, table)
		 
		val jobConfig: JobConf = new JobConf(conf, this.getClass)
		jobConfig.set("mapreduce.output.fileoutputformat.outputdir", "/user/mapr/out")
		jobConfig.setOutputFormat(classOf[TableOutputFormat])
		jobConfig.set(TableOutputFormat.OUTPUT_TABLE, tableName)

		if (args.length < 1)
												{
																println("Needs input directory")
																System.exit(1)
												}

												val sparkConf = new SparkConf().setAppName("DBMCALLDETAILS").set("spark.testing.memory", "536870912")

												val sc = new SparkContext(sparkConf)

												val sqlContext = new SQLContext(sc)

												import sqlContext.implicits._

												val in_path = args(0) + "/*.gz"
												val raw_logs = sc.textFile(in_path).coalesce(100, false)

												val parsed_logs = raw_logs.filter( x => x.length() > 800).map(x => { 
													
													val y = x.substring(3,6).trim()
													
													
													if (y == "001" || y == "002" || y == "029" || y == "030" || y == "031")
													
													{
														logTable(x.substring(3,6).trim(), x.substring(9,24).trim(), x.substring(25,38).trim(), x.substring(38,39).trim(), x.substring(41,57).trim(), x.substring(124,140).trim(), x.substring(179,191).trim(), x.substring(191,196).trim(), x.substring(235,240).trim(), x.substring(241,246).trim(), x.substring(247,258).trim(), x.substring(300,303).trim(), x.substring(326,346).trim(), x.substring(336,340).trim(), x.substring(349,353).trim(), x.substring(420,430).trim(), x.substring(433,443).trim(), x.substring(800,821).trim())
													}
													
													else
													{
														logTable("vxxx", x.substring(9,24).trim(), x.substring(25,38).trim(), x.substring(38,39).trim(), x.substring(41,57).trim(), x.substring(124,140).trim(), x.substring(179,191).trim(), x.substring(191,196).trim(), x.substring(235,240).trim(), x.substring(241,246).trim(), x.substring(247,258).trim(), x.substring(300,303).trim(), x.substring(326,346).trim(), x.substring(336,340).trim(), x.substring(349,353).trim(), x.substring(420,430).trim(), x.substring(433,443).trim(), x.substring(800,821).trim())
													}
													
													
													
													})
												
												
												val IDRDD = parsed_logs.zipWithUniqueId()

												val format = new SimpleDateFormat("yyMMddHHmmss")
												//(rectype == '001') OR ( rectype == '002') OR ( rectype == '029') OR ( rectype == '030') OR ( rectype == '031' )) AND ( transdate != ''") AND (subno != ''")
												val finalTable = IDRDD.filter(_._1.rectype != "vxxx").filter(_._1.transdate != "").filter(_._1.subno != "").map(x => {
															val y = x._2+1
															
															
																val y_string = x._1.subno + x._1.transdate + f"$y%07d"

						
								  //case class logTableID(key: String, transdate: String, subno: String, imei_number: String, cell_id: String, b_subno: String, act_duration: String, call_type: String, imsi_number: String, third_party_no: String, route_in: String, route_out: String, int_flag: String, rectype: String, prepost_flag: String, switch: String, category: String, lac: String, file_name: String)

																
																
																//var p = new Put();
														
																logTableID(y_string, x._1.subno,  x._1.imei_number,  x._1.b_subno)

														  })


														finalTable.take(25).foreach(println)
														
														
														finalTable.map( x => convertToPut(x) ).saveAsHadoopDataset(jobConfig)
														
														def convertToPut(x: logTableID): (ImmutableBytesWritable, Put) = {
															val p = new Put(Bytes.toBytes(x.ID))
															// add columns with data values to put
															p.add(Bytes.toBytes("cf"), Bytes.toBytes("subno"), Bytes.toBytes(x.subno))
															p.add(Bytes.toBytes("cf"), Bytes.toBytes("imei_number"), Bytes.toBytes(x.imei_number))
															p.add(Bytes.toBytes("cf"), Bytes.toBytes("b_subno"), Bytes.toBytes(x.b_subno))
															(new ImmutableBytesWritable, p)
														  }
												
			}

		}
