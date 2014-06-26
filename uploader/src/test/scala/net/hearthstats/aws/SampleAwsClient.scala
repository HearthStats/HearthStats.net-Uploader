package net.hearthstats.aws

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.UUID
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import scala.collection.JavaConversions._
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.ListBucketsRequest

object S3Sample extends App {
  val s3 = new AmazonS3Client(new AWSCredentials {
    val getAWSAccessKeyId = "key"
    val getAWSSecretKey = "secret"
  })
  val usWest2 = Region.getRegion(Regions.US_WEST_2)
  s3.setRegion(usWest2)
  println("Listing buckets")
  for (bucket <- s3.listBuckets(new ListBucketsRequest)) {
    println(" - " + bucket.getName)
  }
  sys.exit()
  val bucketName = "my-first-s3-bucket-" + UUID.randomUUID()
  val key = "MyObjectKey"
  println("Creating bucket " + bucketName + "\n")
  s3.createBucket(bucketName)
  println()
  println("Uploading a new object to S3 from a file\n")
  s3.putObject(new PutObjectRequest(bucketName, key, createSampleFile()))
  println("Downloading an object")
  val `object` = s3.getObject(new GetObjectRequest(bucketName, key))
  println("Content-Type: " + `object`.getObjectMetadata.getContentType)
  displayTextInputStream(`object`.getObjectContent)
  println("Listing objects")
  val objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName)
    .withPrefix("My"))
  for (objectSummary <- objectListing.getObjectSummaries) {
    println(" - " + objectSummary.getKey + "  " + "(size = " + objectSummary.getSize +
      ")")
  }
  println()
  println("Deleting an object\n")
  s3.deleteObject(bucketName, key)
  println("Deleting bucket " + bucketName + "\n")
  s3.deleteBucket(bucketName)

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

  /**
   * Displays the contents of the specified input stream as text.
   *
   * @param input
   *            The input stream to display as text.
   *
   * @throws IOException
   */
  private def displayTextInputStream(input: InputStream) {
    val reader = new BufferedReader(new InputStreamReader(input))
    while (true) {
      val line = reader.readLine()
      if (line == null) //break
        println("    " + line)
    }
    println()
  }
}
