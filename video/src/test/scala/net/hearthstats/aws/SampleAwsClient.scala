package net.hearthstats.aws

import java.io.{ File, FileOutputStream, OutputStreamWriter }

import net.hearthstats.modules.aws.HearthstatsAwsClient;
import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext.Implicits.global

import com.amazonaws.services.s3.model.ListObjectsRequest

object S3Sample extends App {
  val client = new HearthstatsAwsClient(args(0), args(1))
  val s3 = client.s3
  println("Listing buckets")

  for (bucket <- s3.listBuckets) {
    println(" - " + bucket.getName)
  }
  val bucketName = "hearthstats-dev"

  //  client.storeFile(bucketName, "video/DEMO/", new File("""C:\Users\tyrcho\AppData\Local\Temp\video3451076409001028894.mp4""")).onSuccess {
  //    case res => println(s"Success : $res")
  //  }
  client.storeFile(bucketName, "video/REMOVE", createSampleFile()).onSuccess {
    case res => println(s"Success : ${res.getContentMd5}")
  }

  for (objectSummary <- s3.listObjects(new ListObjectsRequest().withBucketName(bucketName)).getObjectSummaries) {
    println(s" - ${objectSummary.getKey} (size = ${objectSummary.getSize})")
  }

  /**
   * Creates a temporary file with text data to demonstrate uploading a file
   * to Amazon S3
   *
   * @return A newly created temporary file with text data.
   *
   * @throws IOException
   */
  private def createSampleFile(): File = {
    val file = File.createTempFile("aws-java-sdk-", ".txt")
    file.deleteOnExit()
    val writer = new OutputStreamWriter(new FileOutputStream(file))
    writer.write("abcdefghijklmnopqrstuvwxyz\n")
    writer.write("01234567890112345678901234\n")
    writer.write("!@#$%^&*()-=[]{};':',.<>/?\n")
    writer.write("01234567890112345678901234\n")
    writer.write("abcdefghijklmnopqrstuvwxyz\n")
    writer.close()
    file
  }
}
