package net.hearthstats.upload

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import net.hearthstats.aws.HearthstatsAwsClient
import grizzled.slf4j.Logging
import java.text.SimpleDateFormat
import java.util.Date

class AwsUploader(
  accessKey: String,
  secretKey: String,
  bucketName: String,
  prefix: String)
  extends FileUploader with Logging {

  val awsClient = new HearthstatsAwsClient(accessKey, secretKey)

  val dateFormat = new SimpleDateFormat("yyyy/MM")

  override def uploadFile(f: File, user: String): Future[Unit] = {
    val dateString = dateFormat.format(new Date)
    val folder = s"$prefix/$user/$dateString/"
    info(s"uploading ${f.getName} to $bucketName in $folder")
    for (_ <- awsClient.storeFile(bucketName, folder, f))
      yield () // we are not doing anything with the result yet
  }
}