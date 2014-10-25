import java.util.ServiceLoader
import net.hearthstats.modules.upload.FileUploader
import java.io.File
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

object SpiSample extends App {

  val ul = ServiceLoader.load(classOf[FileUploader]).iterator.toList.head

  ul.uploadFile(new File(""), "").onComplete { case e => println("done") }
}