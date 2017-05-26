/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.account._
import cards.nine.services.free.domain.{ User, Installation }
import freestyle._

@free trait UserR {
  def add(email: Email, apiKey: ApiKey, sessionToken: SessionToken): FS[Result[User]]
  def addInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): FS[Result[Installation]]
  def getByEmail(email: Email): FS[Result[User]]
  def getBySessionToken(sessionToken: SessionToken): FS[Result[User]]
  def getInstallationByUserAndAndroidId(user: Long, androidId: AndroidId): FS[Result[Installation]]
  def getSubscribedInstallationByCollection(collectionPublicId: String): FS[Result[List[Installation]]]
  def updateInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): FS[Result[Installation]]
}
