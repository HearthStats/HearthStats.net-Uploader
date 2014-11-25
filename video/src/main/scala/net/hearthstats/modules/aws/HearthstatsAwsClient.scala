package net.hearthstats.modules.aws

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.event.{ ProgressEvent, ProgressListener }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ ObjectMetadata, PutObjectRequest, PutObjectResult }

import grizzled.slf4j.Logging

class HearthstatsAwsClient(accessKey: String, secretKey: String) extends Logging {
  val s3 = new AmazonS3Client(new AWSCredentials {
    val getAWSAccessKeyId = accessKey
    val getAWSSecretKey = secretKey
  })

  def storeFile(bucket: String, prefix: String, file: File, name: String): Future[PutObjectResult] = Future {
    val target = s"$prefix/$name"
    info(s"Uploading a new object to S3 from ${file.getName} to $target")
    val metadata = new ObjectMetadata
    metadata.setContentLength(file.length)
    val request = new PutObjectRequest(bucket, target, file)
    request.setMetadata(metadata)
    request.setGeneralProgressListener(new LogProgressListener)
    s3.putObject(request)
  }

  class LogProgressListener extends ProgressListener with Logging {
    var total = 0L
    def progressChanged(progressEvent: ProgressEvent): Unit = {
      val size = progressEvent.getBytesTransferred
      total += size
      debug(total / 1024 + " kb transfered ")
      if (progressEvent.getEventCode == ProgressEvent.COMPLETED_EVENT_CODE) {
        info(" transfer completed  ******")
      }
    }
  }
}
