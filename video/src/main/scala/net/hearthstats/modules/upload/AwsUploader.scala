package net.hearthstats.modules.upload

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import net.hearthstats.modules.aws.HearthstatsAwsClient
import grizzled.slf4j.Logging
import java.text.SimpleDateFormat
import java.util.Date
import net.hearthstats.config.UserConfig
import net.hearthstats.hstatsapi.API

class AwsUploader extends FileUploader with Logging {

  var awsClient: Option[HearthstatsAwsClient] = None

  private def getAwsClient(api: API) = awsClient match {
    case Some(client) => client
    case None =>
      val Seq(accessKey, secretKey) = api.awsKeys
      awsClient = Some(new HearthstatsAwsClient(accessKey, secretKey))
      awsClient.get
  }

  val dateFormat = new SimpleDateFormat("yyyy/MM")

  def uploadFile(f: File, config: UserConfig, api: API): Future[Unit] = {
    val bucketName = config.awsBucket.get
    val prefix = config.awsVideoPrefix.get
    val dateString = dateFormat.format(new Date)
    val user = api.premiumUserId.get
    val folder = s"$prefix/$user/$dateString/"
    info(s"uploading ${f.getName} to $bucketName in $folder")
    for (_ <- getAwsClient(api).storeFile(bucketName, folder, f))
      yield () // we are not doing anything with the result yet
  }
}