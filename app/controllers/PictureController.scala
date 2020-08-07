package controllers

import java.nio.file.{FileSystems, Files, Path, StandardCopyOption}
import java.time.{Clock, LocalDateTime}
import javax.inject.{Inject, Singleton}

import com.google.common.net.MediaType
import domain.entity.{PictureProperty, TwitterId}
import play.api.cache.SyncCacheApi
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PicturesController @Inject()(
                                    cc: ControllerComponents,
                                    clock: Clock,
                                    executionContext: ExecutionContext,
                                    val cache: SyncCacheApi,
                                  ) extends TwitterLoginController(cc) {

  implicit val ec = executionContext
  val originalStoreDirPath = "./filesystem/original"

  def post = TwitterLoginAction.async { request =>
    (request.accessToken, request.body.asMultipartFormData) match {
      case (Some(accessToken), Some(form)) =>
        form.file("file") match {
          case Some(file) =>
            val storeDirPath = FileSystems.getDefault.getPath(originalStoreDirPath)
            if (!Files.exists(storeDirPath)) Files.createDirectories(storeDirPath)

            val originalFilepath =  FileSystems.getDefault.getPath(storeDirPath.toString, System.currentTimeMillis().toString)
            Files.copy(file.ref.path, originalFilepath, StandardCopyOption.COPY_ATTRIBUTES)
            val propertyValue = createPicturePropertyValue(TwitterId(accessToken.getUserId), file, form, originalFilepath)

            println(propertyValue)
            // TODO MySQLへのプロパティ保存とRedisへのタスクの保存

            Future.successful(Ok("Picture uploaded."))
          case _ => Future.successful(Unauthorized("Need picture data."))
        }
      case _ => Future.successful(Unauthorized("Need to login by Twitter and picture data."))
    }
  }

  private[this] def createPicturePropertyValue(
                                                twitterId: TwitterId,
                                                file: FilePart[TemporaryFile],
                                                form: MultipartFormData[TemporaryFile],
                                                originalFilePath: Path
                                              ): PictureProperty.Value = {
    val overlayText = form.dataParts.get("overlaytext").flatMap(_.headOption).getOrElse("")
    val overlayTextSize = form.dataParts.get("overlaytextsize").flatMap(_.headOption).getOrElse("60").toInt
    val contentType = MediaType.parse(file.contentType.getOrElse("application/octet-stream"))

    PictureProperty.Value(
      PictureProperty.Status.Converting,
      twitterId,
      file.filename,
      contentType,
      overlayText,
      overlayTextSize,
      Some(originalFilePath.toString),
      None,
      LocalDateTime.now(clock))
  }

}
