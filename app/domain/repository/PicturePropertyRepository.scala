package domain.repository


import domain.entity.{PictureId, PictureProperty}

import scala.concurrent.Future

trait PicturePropertyRepository {
  /**
    * 画像のプロパティを保存する
    *
    * @param value 画像のプロパティの値
    * @return Future.successful(PictureId)     新しく割り当てられた画像ID
    */
  def create(value: PictureProperty.Value): Future[PictureId]

}
